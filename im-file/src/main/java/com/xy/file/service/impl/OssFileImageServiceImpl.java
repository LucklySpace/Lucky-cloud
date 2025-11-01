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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 图片文件服务实现类
 * 提供图片上传、处理、存储等功能
 */
@Slf4j
@Service
public class OssFileImageServiceImpl implements OssFileImageService {

    /**
     * 头像存储桶名称
     */
    public static final String AVATAR_BUCKET_NAME = "avatar";
    private static final String WATERMARK_IMAGE = "watermark";
    private static final String COMPRESS_IMAGE = "compress";
    /**
     * 策略键值定义
     */
    private static final String THUMBNAIL_IMAGE = "thumbnail";
    /**
     * 内存/磁盘切换阈值：5MB
     * 小于等于该值使用内存处理，否则使用临时文件处理
     */
    private static final long IN_MEMORY_THRESHOLD = 5 * 1024 * 1024L;
    /**
     * IO缓冲区大小：16KB
     */
    private static final int IO_BUFFER_SIZE = 16 * 1024;
    /**
     * 头像存储桶是否已设置为公开访问
     */
    private static final AtomicBoolean avatarBucketIsPublic = new AtomicBoolean(false);

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private RedisRepo redisRepo;

    @Resource
    private MinioProperties minioProperties;

    /**
     * NSFW检测API地址
     */
    @Value("${nsfw.api.url:http://localhost:3000/classify}")
    private String nsfwApiUrl;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 异步任务执行器
     */
    @Resource(name = "asyncServiceExecutor")
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    /**
     * 图片处理策略映射
     */
    @Autowired
    private Map<String, ImageProcessingStrategy> imageProcessingStrategyMap;

    /**
     * 将供应器的流一次性读取为字节数组
     * 适用于策略库需要字节数组输入的场景
     *
     * @param supplier 输入流供应器
     * @return 字节数组
     * @throws IOException 流操作异常
     */
    private static byte[] streamToBytes(Supplier<InputStream> supplier) throws IOException {
        try (InputStream is = supplier.get()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
            byte[] buf = new byte[IO_BUFFER_SIZE];
            int r;
            while ((r = is.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return baos.toByteArray();
        }
    }

    /**
     * 将MultipartFile保存为临时文件
     * 仅在大文件模式下使用
     *
     * @param multipartFile 原始文件
     * @return 临时文件对象
     * @throws IOException 文件操作异常
     */
    private File toTempFile(MultipartFile multipartFile) throws IOException {
        File tmp = Files.createTempFile(
                        "oss_img_",
                        "_" + Objects.requireNonNull(multipartFile.getOriginalFilename()))
                .toFile();

        try (InputStream is = multipartFile.getInputStream();
             OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp), IO_BUFFER_SIZE)) {
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
     * 若文件较小则读入内存并返回byte[]，否则返回null（表示使用临时文件路径）
     *
     * @param file 原始文件
     * @return 文件内容的字节数组或null
     * @throws IOException 文件操作异常
     */
    private byte[] tryReadToMemory(MultipartFile file) throws IOException {
        long size = file.getSize();
        if (size <= 0 || size > IN_MEMORY_THRESHOLD) {
            return null;
        }

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
     *
     * @param file 图片文件
     * @return 上传结果
     */
    @Override
    public Result<?> uploadImage(MultipartFile file) {
        log.info("[文件上传] 图片文件处理");

        // 基本检查
        if (file == null || file.isEmpty()) {
            return Result.failed("文件为空");
        }

        long start = System.nanoTime();

        // 封装返回对象
        OssFileImage ossFile = new OssFileImage();
        OssFileMediaInfo mediaInfo = new OssFileMediaInfo();

        // 生成对象名称和存储桶名称
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), file.getOriginalFilename());
        String bucketName = minioUtils.getOrCreateBucketByFileType("image");

        // 准备数据源供应器：内存字节数组或临时文件
        byte[] inMemory = null;
        File tempFile = null;
        Supplier<InputStream> sourceSupplier = null;

        try {
            inMemory = tryReadToMemory(file);
            if (inMemory != null) {
                // 小文件使用内存处理
                byte[] finalInMemory = inMemory;
                sourceSupplier = () -> new ByteArrayInputStream(finalInMemory);
            } else {
                // 大文件写入临时文件，并从该文件重新打开流
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

            // 如果启用了NSFW检查，则阻塞直到检查完成
            if (minioProperties.getIsChecked()) {
                ResponseEntity<?> checkRes = checkImageUsingSupplier(sourceSupplier, file.getSize(), file.getOriginalFilename());

                // 检查响应状态
                if (!checkRes.getStatusCode().is2xxSuccessful()) {
                    return Result.failed(ResultCode.FORBIDDEN, "图片校验未通过: " + checkRes.getBody());
                }

                // 检查响应体中的违规内容
                Object body = checkRes.getBody();
                if (body instanceof Map) {
                    Object valid = ((Map<?, ?>) body).get("valid");
                    if (Boolean.TRUE.equals(valid)) {
                        return Result.failed(ResultCode.FORBIDDEN, "图片包含违规内容: " + body);
                    }
                }
            }

            // 主处理流程供应器，每次使用都需要提供新的流
            Supplier<InputStream> mainSupplier = sourceSupplier;

            // 异步处理主图：应用水印和压缩（按顺序处理）
            CompletableFuture<byte[]> mainProcessedFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] baseBytes = streamToBytes(mainSupplier);

                    // 应用水印
                    if (minioProperties.getCreateWatermark()) {
                        baseBytes = processWithStrategyBytes(baseBytes, WATERMARK_IMAGE, mediaInfo);
                    }

                    // 应用压缩
                    if (minioProperties.getIsCompress()) {
                        baseBytes = processWithStrategyBytes(baseBytes, COMPRESS_IMAGE, mediaInfo);
                    }

                    return baseBytes;
                } catch (Exception ex) {
                    log.error("主图处理异常", ex);
                    throw new RuntimeException(ex);
                }
            }, applicationTaskExecutor);

            // 异步生成缩略图（可与主图处理并行执行）
            CompletableFuture<String> thumbnailPathFuture = CompletableFuture.completedFuture(null);
            if (minioProperties.getCreateThumbnail()) {
                Supplier<InputStream> finalSourceSupplier = sourceSupplier;
                thumbnailPathFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] thumbnailBytes = processWithStrategyBytes(
                                streamToBytes(finalSourceSupplier), THUMBNAIL_IMAGE, mediaInfo);

                        // 上传缩略图到存储桶
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

            // 主图处理完成后上传
            CompletableFuture<String> mainUploadFuture = mainProcessedFuture.thenApplyAsync(mainBytes -> {
                try (InputStream is = new ByteArrayInputStream(mainBytes)) {
                    minioUtils.uploadFile(bucketName, objectName, is, "image/png");
                } catch (IOException e) {
                    log.error("上传主图失败", e);
                    throw new RuntimeException(e);
                }
                return minioUtils.getFilePath(bucketName, objectName);
            }, applicationTaskExecutor);

            // 等待所有任务完成
            CompletableFuture<Void> combined = CompletableFuture.allOf(mainUploadFuture, thumbnailPathFuture);
            combined.join();

            String mainPath = mainUploadFuture.join();
            String thumbPath = thumbnailPathFuture.join();

            ossFile.setPath(mainPath);
            if (thumbPath != null) {
                ossFile.setThumbnailPath(thumbPath);
            }

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
            // 清理临时文件
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
     * 上传头像文件
     *
     * @param file 头像文件
     * @return 上传结果
     */
    @Override
    public Result uploadAvatar(MultipartFile file) {
        log.info("[文件上传] 头像文件处理");

        // 基本检查
        if (file == null || file.isEmpty()) {
            return Result.failed("文件为空");
        }

        long start = System.nanoTime();

        // 封装返回对象
        OssFileImage ossFile = new OssFileImage();

        // 头像使用专门的公开存储桶
        String bucketName = minioUtils.getOrCreateBucketByFileType(AVATAR_BUCKET_NAME);

        // 判断头像桶是否已设置为公开访问
        if (!avatarBucketIsPublic.get()) {
            try {
                // 设置为公开存储桶
                boolean bucketIsPublic = minioUtils.setBucketPublic(bucketName);
                if (bucketIsPublic) {
                    avatarBucketIsPublic.set(true);
                }
            } catch (Exception e) {
                log.warn("设置头像桶公开策略失败，bucketName: {}", bucketName, e);
            }
        }

        // 生成对象名称
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {

            // 直接上传头像文件，不需要额外处理
            minioUtils.uploadFile(bucketName, objectName, inputStream, file.getContentType());

            // 获取文件访问路径
            ossFile.setPath(minioUtils.getPublicFilePath(bucketName, objectName));

            log.info("头像上传处理完成 file={}, elapsedMs={}", file.getOriginalFilename(),
                    (System.nanoTime() - start) / 1_000_000L);
            return Result.success(ossFile);
        } catch (Exception e) {
            log.error("头像上传处理异常", e);
            return Result.failed(ResultCode.SERVICE_EXCEPTION, "头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 基于字节数组调用策略处理后返回处理后的字节数组
     *
     * @param in 输入字节数组
     * @param strategyKey 策略键
     * @param mediaInfo 媒体信息对象
     * @return 处理后的字节数组
     * @throws Exception 处理异常
     */
    private byte[] processWithStrategyBytes(byte[] in, String strategyKey, OssFileMediaInfo mediaInfo)
            throws Exception {
        ImageProcessingStrategy strategy = imageProcessingStrategyMap.get(strategyKey);
        if (strategy == null) {
            return in;
        }

        try (InputStream is = new ByteArrayInputStream(in);
             ByteArrayOutputStream os = new ByteArrayOutputStream(Math.max(in.length / 2, 4096))) {
            try (InputStream processed = strategy.process(is, mediaInfo)) {
                byte[] buf = new byte[IO_BUFFER_SIZE];
                int r;
                while ((r = processed.read(buf)) != -1) {
                    os.write(buf, 0, r);
                }
            }
            return os.toByteArray();
        }
    }

    /**
     * 流式校验图片（使用InputStreamResource直接POST）
     * 保持原来同步等待校验结果的语义，以保证不合规图片不会上传
     *
     * @param supplier 输入流供应器（可重复打开）
     * @param size 文件大小（用于InputStreamResource的content-length）
     * @param filename 原始文件名（用于表单）
     * @return 校验响应
     */
    public ResponseEntity<?> checkImageUsingSupplier(Supplier<InputStream> supplier, long size, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 创建InputStreamResource并暴露文件名和内容长度
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
            Map<String, Double> result = response.getBody();
            if (result == null || result.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "未收到有效的分类结果！"));
            }

            // 检查是否违规：成人、18禁动画、性感内容
            String[] violatingCategories = {"porn", "hentai", "sexy"};
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
     * @param fileName 文件名
     * @return 访问地址
     */
    public String generatorUrl(String bucketName, String fileName) {
        return minioProperties.getExtranet() + "/" + bucketName + "/" + fileName;
    }

    /**
     * 处理图片（旧版兼容方法）
     *
     * @param fileBytes 文件字节数组
     * @param ossMediaFileInfo 媒体信息
     * @param strategyKey 策略键
     * @return 处理后的字节数组
     * @throws Exception 处理异常
     */
    private byte[] processImage(byte[] fileBytes, OssFileMediaInfo ossMediaFileInfo, String strategyKey)
            throws Exception {
        return processWithStrategyBytes(fileBytes, ossMediaFileInfo, strategyKey);
    }

    /**
     * 基于字节数组调用策略处理后返回处理后的字节数组（旧版兼容方法）
     *
     * @param fileBytes        输入字节数组
     * @param ossMediaFileInfo 媒体信息对象
     * @param strategyKey      策略键
     * @return 处理后的字节数组
     * @throws Exception 处理异常
     */
    private byte[] processWithStrategyBytes(byte[] fileBytes, OssFileMediaInfo ossMediaFileInfo, String strategyKey)
            throws Exception {
        ImageProcessingStrategy strategy = imageProcessingStrategyMap.get(strategyKey);
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            InputStream processedStream = strategy.process(inputStream, ossMediaFileInfo);
            processedStream.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }
}