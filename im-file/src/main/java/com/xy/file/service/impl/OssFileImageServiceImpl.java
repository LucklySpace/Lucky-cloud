package com.xy.file.service.impl;

import com.xy.file.client.MinioProperties;
import com.xy.file.domain.OssFileImage;
import com.xy.file.domain.OssFileMediaInfo;
import com.xy.file.handler.ImageProcessingStrategy;
import com.xy.file.service.OssFileImageService;
import com.xy.file.util.MinioUtils;
import com.xy.file.util.RedisRepo;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
@Service
public class OssFileImageServiceImpl implements OssFileImageService {

    // 策略 key
    private static final String THUMBNAIL_IMAGE = "thumbnail";
    private static final String WATERMARK_IMAGE = "watermark";
    private static final String COMPRESS_IMAGE = "compress";

    // 内存/磁盘切换阈值：5MB（小于等于该值使用内存，否则使用临时文件）
    private static final long IN_MEMORY_THRESHOLD = 5 * 1024 * 1024L;
    private static final int IO_BUFFER_SIZE = 16 * 1024; // 16KB buffer

    //private final Map<String, ImageProcessingStrategy> strategies = new HashMap<>();

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private RedisRepo redisRepo;

    @Resource
    private MinioProperties minioProperties;

    @Value("${nsfw.api.url:http://localhost:3000/classify}")
    private String nsfwApiUrl;

    @Resource
    private RestTemplate restTemplate;

    @Resource(name = "asyncServiceExecutor")
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    @Autowired
    private Map<String, ImageProcessingStrategy> imageProcessingStrategyMap;

    /**
     * 将 MultipartFile 保存为临时文件（仅在大文件模式下使用）
     */
    private File toTempFile(MultipartFile multipartFile) throws IOException {
        File tmp = Files.createTempFile("oss_img_", "_" + Objects.requireNonNull(multipartFile.getOriginalFilename()))
                .toFile();
        try (InputStream is = multipartFile.getInputStream(); OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp), IO_BUFFER_SIZE)) {
            byte[] buf = new byte[IO_BUFFER_SIZE];
            int r;
            while ((r = is.read(buf)) != -1) {
                os.write(buf, 0, r);
            }
            os.flush();
        }
        return tmp;
    }

    /**
     * 若文件较小则读入内存并返回 byte[]，否则返回 null（表示使用 temp-file path）
     */
    private byte[] tryReadToMemory(MultipartFile file) throws IOException {
        long size = file.getSize();
        if (size <= 0 || size > IN_MEMORY_THRESHOLD) return null;
        // 预设大小
        int initial = (int) Math.max(1024, Math.min(size, 16 * 1024));
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(initial);
             InputStream is = new BufferedInputStream(file.getInputStream(), IO_BUFFER_SIZE)) {
            byte[] buf = new byte[IO_BUFFER_SIZE];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toByteArray();
        }
    }

    /**
     * 上传图片主入口
     */
    @Override
    public Result<?> uploadImage(MultipartFile file) {
        // minimal checks
        if (file == null || file.isEmpty()) {
            return Result.failed("文件为空");
        }

        long start = System.nanoTime();

        // 封装返回对象
        OssFileImage ossFile = new OssFileImage();
        OssFileMediaInfo mediaInfo = new OssFileMediaInfo();

        // objectName & bucket
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), file.getOriginalFilename());

        String bucketName = minioUtils.getOrCreateBucketByFileType("image");

        // prepare source supplier: either byte[] or temp file
        byte[] inMemory = null;
        File tempFile = null;
        Supplier<InputStream> sourceSupplier = null;
        try {
            inMemory = tryReadToMemory(file);
            if (inMemory != null) {
                // small file in-memory
                byte[] finalInMemory = inMemory;
                sourceSupplier = () -> new ByteArrayInputStream(finalInMemory);
            } else {
                // large file -> write to temp file once, and reopen streams from it
                tempFile = toTempFile(file);
                File tf = tempFile;
                sourceSupplier = () -> {
                    try {
                        return new BufferedInputStream(new FileInputStream(tf), IO_BUFFER_SIZE);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                };
            }

            // 1) Block until NSFW check done if enabled (to preserve original behavior).
            if (minioProperties.getIsChecked()) {
                // checkImage uses streaming upload (no temp file) and will block here
                ResponseEntity<?> checkRes = checkImageUsingSupplier(sourceSupplier, file.getSize(), file.getOriginalFilename());
                // expected body may contain valid flag; adapt behavior (fail if invalid)
                if (!checkRes.getStatusCode().is2xxSuccessful()) {
                    return Result.failed(ResultCode.FORBIDDEN, "图片校验未通过: " + checkRes.getBody());
                }
                // If check API returns a map with "valid": true meaning violation (legacy code had inverted semantics),
                // user may adapt. For now: if body contains Map with "valid"==true => forbid.
                Object body = checkRes.getBody();
                if (body instanceof Map) {
                    Object valid = ((Map<?, ?>) body).get("valid");
                    if (Boolean.TRUE.equals(valid)) {
                        return Result.failed(ResultCode.FORBIDDEN, "图片包含违规内容 :" + body);
                    }
                }
            }

            // main pipeline supplier must provide fresh stream per use
            Supplier<InputStream> mainSupplier = sourceSupplier;

            CompletableFuture<byte[]> mainProcessedFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    // apply watermark then compress if configured (sequential on main image)
                    byte[] baseBytes = streamToBytes(mainSupplier);
                    // watermark
                    if (minioProperties.getCreateWatermark()) {
                        baseBytes = processWithStrategyBytes(baseBytes, WATERMARK_IMAGE, mediaInfo);
                    }
                    // compress
                    if (minioProperties.getIsCompress()) {
                        baseBytes = processWithStrategyBytes(baseBytes, COMPRESS_IMAGE, mediaInfo);
                    }
                    return baseBytes;
                } catch (Exception ex) {
                    log.error("主图处理异常", ex);
                    throw new RuntimeException(ex);
                }
            }, applicationTaskExecutor);

            // thumbnail generation (can run in parallel)
            CompletableFuture<String> thumbnailPathFuture = CompletableFuture.completedFuture(null);
            if (minioProperties.getCreateThumbnail()) {
                Supplier<InputStream> finalSourceSupplier = sourceSupplier;
                thumbnailPathFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] thumbnailBytes = processWithStrategyBytes(streamToBytes(finalSourceSupplier), THUMBNAIL_IMAGE, mediaInfo);
                        // upload thumbnail to bucket with suffix
                        String thumbName = objectName + ".thumb.png";
                        try (InputStream is = new ByteArrayInputStream(thumbnailBytes)) {
                            minioUtils.uploadFile(bucketName, thumbName, is, "image/png");
                        }
                        return minioUtils.getFilePath(bucketName, thumbName);
                    } catch (Exception e) {
                        log.error("生成/上传缩略图失败", e);
                        return null;
                    }
                }, applicationTaskExecutor);
            }

            // upload main image once processed
            CompletableFuture<String> mainUploadFuture = mainProcessedFuture.thenApplyAsync(mainBytes -> {
                try (InputStream is = new ByteArrayInputStream(mainBytes)) {
                    minioUtils.uploadFile(bucketName, objectName, is, "image/png");
                } catch (IOException e) {
                    log.error("上传主图失败", e);
                    throw new RuntimeException(e);
                }
                return minioUtils.getFilePath(bucketName, objectName);
            }, applicationTaskExecutor);

            // set OSS file fields after both uploaded
            CompletableFuture<Void> combined = CompletableFuture.allOf(mainUploadFuture, thumbnailPathFuture);
            combined.join(); // wait for both to complete - blocking here but processing/upload performed in parallel threads

            String mainPath = mainUploadFuture.join();
            String thumbPath = thumbnailPathFuture.join();

            ossFile.setPath(mainPath);
            if (thumbPath != null) ossFile.setThumbnailPath(thumbPath);

            long elapsed = (System.nanoTime() - start) / 1_000_000L;
            log.info("图片上传处理完成 file={}, elapsedMs={}", file.getOriginalFilename(), elapsed);
            return Result.success(ossFile);
        } catch (RuntimeException re) {
            log.error("上传处理失败", re);
            return Result.failed(ResultCode.SERVICE_EXCEPTION, "图片上传失败: " + re.getMessage());
        } catch (Exception e) {
            log.error("上传处理异常", e);
            return Result.failed(ResultCode.SERVICE_EXCEPTION, "图片上传失败: " + e.getMessage());
        } finally {
            // cleanup temp file if created
            // important: cleanup even if upload succeeded (we use tempFile only as source)
            // Do it asynchronously to avoid blocking response if file is large.
            if (tempFile != null && tempFile.exists()) {
                File tf = tempFile;
                CompletableFuture.runAsync(() -> {
                    try {
                        Files.deleteIfExists(tf.toPath());
                    } catch (Exception ex) {
                        log.warn("删除临时文件失败: {}", tf.getAbsolutePath(), ex);
                    }
                }, applicationTaskExecutor);
            }
        }
    }

    /**
     * 将 supplier 的流一次性读取为 byte[]（适用于策略库需要 byte[] 输入）
     */
    private static byte[] streamToBytes(Supplier<InputStream> supplier) throws IOException {
        try (InputStream is = supplier.get()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            byte[] buf = new byte[IO_BUFFER_SIZE];
            int r;
            while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
            return baos.toByteArray();
        }
    }

    /**
     * 基于 byte[] 调用策略后的返回 byte[]
     */
    private byte[] processWithStrategyBytes(byte[] in, String strategyKey, OssFileMediaInfo mediaInfo) throws Exception {
        ImageProcessingStrategy strategy = imageProcessingStrategyMap.get(strategyKey);
        if (strategy == null) return in;
        try (InputStream is = new ByteArrayInputStream(in);
             ByteArrayOutputStream os = new ByteArrayOutputStream(Math.max(in.length / 2, 4096))) {
            try (InputStream processed = strategy.process(is, mediaInfo)) {
                byte[] buf = new byte[IO_BUFFER_SIZE];
                int r;
                while ((r = processed.read(buf)) != -1) os.write(buf, 0, r);
            }
            return os.toByteArray();
        }
    }

    /**
     * 流式校验图片（使用 InputStreamResource 直接 POST）
     * 保持原来同步等待校验结果的语义，以保证不合规图片不会上传。（若需要高吞吐，请改为异步校验+回滚）
     *
     * @param supplier input stream supplier（可重复打开）
     * @param size     大小（用于 InputStreamResource 的 content-length）
     * @param filename 原始文件名（用于表单）
     */
    public ResponseEntity<?> checkImageUsingSupplier(Supplier<InputStream> supplier, long size, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // InputStreamResource that exposes filename and contentLength if possible
            InputStreamResource resource = new InputStreamResource(supplier.get()) {
                @Override
                public String getFilename() {
                    return filename != null ? filename : "file";
                }

                @Override
                public long contentLength() {
                    return size >= 0 && size <= Integer.MAX_VALUE ? size : -1;
                }
            };

            body.add("image", resource);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用分类接口
            ResponseEntity<Map> response = restTemplate.postForEntity(nsfwApiUrl, requestEntity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(response.getStatusCode()).body("分类接口请求失败！");
            }

            // 解析响应
            Map<String, Double> result = (Map<String, Double>) response.getBody();
            if (result == null || result.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "未收到有效的分类结果！"));
            }

            // 检查是否违规
            String[] violatingCategories = {"porn", "hentai", "sexy"};  // 成人，18禁动画，性
            List<String> violations = new ArrayList<>();
            for (String category : violatingCategories) {
                if (result.containsKey(category) && result.get(category) >= 0.5) {
                    violations.add(String.format("类别: %s, 概率: %.2f", category, result.get(category)));
                }
            }

            if (!violations.isEmpty()) {
                return ResponseEntity.ok(Map.of("valid", true, "result", violations));
            }

            return ResponseEntity.ok(Map.of("valid", false, "result", violations));
        } catch (Exception e) {
            log.error("checkImageUsingSupplier error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("校验异常: " + e.getMessage());
        }
    }

    /**
     * 生成文件访问地址
     *
     * @param bucketName 存储桶名
     * @param fileName   文件名
     * @return 访问地址
     */
    public String generatorUrl(String bucketName, String fileName) {
        return minioProperties.getExtranet() + "/" + bucketName + "/" + fileName;
    }

    // --------------- 以下为原始辅助方法（保留，仅作调用） ----------------

    /**
     * 若你还需要单独的 processImage(old style) ，保留并兼容
     */
    private byte[] processImage(byte[] fileBytes, OssFileMediaInfo ossMediaFileInfo, String strategyKey) throws Exception {
        return processWithStrategyBytes(fileBytes, ossMediaFileInfo, strategyKey);
    }

    private byte[] processWithStrategyBytes(byte[] fileBytes, OssFileMediaInfo ossMediaFileInfo, String strategyKey) throws Exception {
        ImageProcessingStrategy strategy = imageProcessingStrategyMap.get(strategyKey);
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            InputStream processedStream = strategy.process(inputStream, ossMediaFileInfo);
            processedStream.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }
}


