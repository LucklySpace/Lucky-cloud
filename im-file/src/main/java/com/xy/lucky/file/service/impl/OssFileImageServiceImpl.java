package com.xy.lucky.file.service.impl;

import com.xy.lucky.file.client.MinioProperties;
import com.xy.lucky.file.domain.OssFileImage;
import com.xy.lucky.file.domain.OssFileMediaInfo;
import com.xy.lucky.file.domain.po.OssFileImagePo;
import com.xy.lucky.file.domain.vo.FileVo;
import com.xy.lucky.file.exception.FileException;
import com.xy.lucky.file.handler.ImageProcessingStrategy;
import com.xy.lucky.file.mapper.FileVoMapper;
import com.xy.lucky.file.mapper.OssFileImageEntityMapper;
import com.xy.lucky.file.repository.OssFileImageRepository;
import com.xy.lucky.file.service.OssFileImageService;
import com.xy.lucky.file.util.MinioUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 图片服务实现（简洁版）
 * 主要职责：
 *  - 小文件内存处理 / 大文件临时文件处理
 *  - 可选 NSFW 校验
 *  - 水印/压缩策略链处理
 *  - 主图与缩略图异步生成并上传
 *  - 头像上传到公开桶
 */
@Slf4j
@Service
public class OssFileImageServiceImpl implements OssFileImageService {

    public static final String AVATAR_BUCKET_NAME = "avatar";
    private static final String THUMBNAIL_KEY = "thumbnail";
    private static final String WATERMARK_KEY = "watermark";
    private static final String COMPRESS_KEY = "compress";

    // 5MB 阈值：小于等于内存处理，大文件写临时文件
    private static final long IN_MEMORY_THRESHOLD = 5 * 1024 * 1024L;
    private static final int IO_BUFFER = 16 * 1024;

    private static final AtomicBoolean avatarBucketIsPublic = new AtomicBoolean(false);

    @Resource
    private MinioUtils minioUtils;
    @Resource
    private MinioProperties minioProperties;
    @Resource
    private OssFileImageRepository ossFileImageRepository;
    @Resource
    private OssFileImageEntityMapper ossFileImageEntityMapper;
    @Resource
    private FileVoMapper fileVoMapper;
    @Resource
    private RestTemplate restTemplate;
    @Resource(name = "asyncServiceExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private Map<String, ImageProcessingStrategy> imageProcessingStrategyMap;

    @Value("${nsfw.api.url:http://localhost:3000/classify}")
    private String nsfwApiUrl;


    /**
     *  上传图片
     * @param file 图片文件
     * @return 文件信息
     */
    @Override
    public FileVo uploadImage(MultipartFile file) {
        log.info("uploadImage start: name={}, size={}", file == null ? null : file.getOriginalFilename(),
                file == null ? 0 : file.getSize());
        if (file == null || file.isEmpty()) {
            throw new FileException("文件为空");
        }

        long start = System.nanoTime();
        String bucket = minioUtils.getOrCreateBucketByFileType("image");
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), file.getOriginalFilename());

        // 先建立数据源 supplier（内存或临时文件）
        File tmp = null;
        Supplier<InputStream> supplier = null;
        try {
            byte[] mem = tryReadToMemory(file);
            if (mem != null) {
                supplier = () -> new ByteArrayInputStream(mem);
            } else {
                tmp = toTempFile(file);
                File tempRef = tmp;
                supplier = () -> {
                    try {
                        return new BufferedInputStream(new FileInputStream(tempRef), IO_BUFFER);
                    } catch (FileNotFoundException e) {
                        throw new UncheckedIOException(e);
                    }
                };
            }

            // 可选 NSFW 校验（会读取一次流）
            if (minioProperties.getIsChecked()) {
                ResponseEntity<?> check = checkImageUsingSupplier(supplier, file.getSize(), file.getOriginalFilename());
                if (!check.getStatusCode().is2xxSuccessful()) {
                    throw new FileException("图片校验接口异常");
                }
                Object body = check.getBody();
                if (body instanceof Map) {
                    Object valid = ((Map<?, ?>) body).get("valid");
                    if (Boolean.TRUE.equals(valid)) {
                        throw new FileException("图片包含违规内容");
                    }
                }
            }

            // media info 容器，可由策略填充
            OssFileMediaInfo mediaInfo = new OssFileMediaInfo();

            // 主图处理（异步）：先把 supplier 流读成 bytes，再按策略处理
            final Supplier<InputStream> mainSupplier = supplier;
            CompletableFuture<byte[]> mainProcessed = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] base = toBytes(mainSupplier);
                    return applyStrategies(base, mediaInfo);
                } catch (Exception ex) {
                    log.error("主图处理失败", ex);
                    throw new FileException("主图处理失败: " + ex.getMessage());
                }
            }, executor);

            // 缩略图（可选）并行生成上传
            CompletableFuture<String> thumbFuture = CompletableFuture.completedFuture(null);
            if (minioProperties.getCreateThumbnail()) {
                final Supplier<InputStream> thumbSupplier = supplier;
                thumbFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] base = toBytes(thumbSupplier);
                        byte[] thumb = processWithStrategy(base, THUMBNAIL_KEY, mediaInfo);
                        String thumbName = objectName + ".thumb.png";
                        uploadBytes(bucket, thumbName, thumb, "image/png");
                        return minioUtils.getFilePath(bucket, thumbName);
                    } catch (Exception e) {
                        log.error("缩略图生成/上传失败", e);
                        return null;
                    }
                }, executor);
            }

            // 主图上传（等待处理完成）
            CompletableFuture<String> mainUpload = mainProcessed.thenApplyAsync(bytes -> {
                try {
                    uploadBytes(bucket, objectName, bytes, "image/png");
                    return minioUtils.getFilePath(bucket, objectName);
                } catch (IOException e) {
                    log.error("主图上传失败", e);
                    throw new FileException("主图上传失败: " + e.getMessage());
                }
            }, executor);

            // 等待完成
            CompletableFuture.allOf(mainUpload, thumbFuture).join();

            String mainPath = mainUpload.join();
            String thumbPath = thumbFuture.join();

            OssFileImage doc = new OssFileImage();
            doc.setBucketName(bucket);
            doc.setFileName(file.getOriginalFilename());
            doc.setObjectKey(objectName);
            doc.setFileType("image");
            doc.setContentType("image/png");
            doc.setFileSize(file.getSize());
            doc.setPath(mainPath);
            if (thumbPath != null) {
                doc.setThumbnailPath(thumbPath);
            }
            FileVo result = persistAndReturn(doc);

            log.info("uploadImage done: name={}, elapsedMs={}", file.getOriginalFilename(),
                    (System.nanoTime() - start) / 1_000_000L);

            return result;
        } catch (Exception ex) {
            log.error("uploadImage error", ex);
            throw new FileException("图片上传失败: " + ex.getMessage());
        } finally {
            // 清理临时文件（异步）
            if (tmp != null && tmp.exists()) {
                File t = tmp;
                CompletableFuture.runAsync(() -> {
                    try {
                        Files.deleteIfExists(t.toPath());
                    } catch (Exception ex) {
                        log.warn("删除临时文件失败: {}", t.getAbsolutePath(), ex);
                    }
                }, executor);
            }
        }
    }

    /**
     * 上传头像
     * @param file 头像文件
     * @return 文件信息
     */
    @Override
    public FileVo uploadAvatar(MultipartFile file) {
        log.info("uploadAvatar start: name={}, size={}", file == null ? null : file.getOriginalFilename(),
                file == null ? 0 : file.getSize());
        if (file == null || file.isEmpty()) {
            throw new FileException("文件为空");
        }

        String bucket = minioUtils.getOrCreateBucketByFileType(AVATAR_BUCKET_NAME);
        try {
            // 尝试只做一次公开设置
            if (!avatarBucketIsPublic.get()) {
                try {
                    if (minioUtils.setBucketPublic(bucket)) avatarBucketIsPublic.set(true);
                } catch (Exception e) {
                    log.warn("设置头像桶公开失败: {}", bucket, e);
                }
            }
            String objectName = minioUtils.getObjectName(minioUtils.generatePath(), file.getOriginalFilename());
            try (InputStream is = file.getInputStream()) {
                minioUtils.uploadFile(bucket, objectName, is, file.getContentType());
            }
            OssFileImage doc = new OssFileImage();
            doc.setBucketName(bucket);
            doc.setFileName(file.getOriginalFilename());
            doc.setObjectKey(objectName);
            doc.setFileType("image");
            doc.setContentType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setPath(minioUtils.getPublicFilePath(bucket, objectName));
            FileVo result = persistAndReturn(doc);
            log.info("uploadAvatar done: name={}", file.getOriginalFilename());
            return result;
        } catch (Exception e) {
            log.error("uploadAvatar error", e);
            throw new FileException("头像上传失败: " + e.getMessage());
        }
    }

    // ------------------------ 内部/工具方法 ------------------------

    /**
     * 将 supplier 的流读为 bytes（每次调用都新建流）
     */
    private byte[] toBytes(Supplier<InputStream> supplier) throws IOException {
        try (InputStream is = supplier.get();
             ByteArrayOutputStream os = new ByteArrayOutputStream(Math.max(4096, IO_BUFFER))) {
            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            return os.toByteArray();
        }
    }

    /**
     * 按策略链处理字节（例如水印、压缩）
     */
    private byte[] applyStrategies(byte[] src, OssFileMediaInfo mediaInfo) throws Exception {
        if (minioProperties.getCreateWatermark()) src = processWithStrategy(src, WATERMARK_KEY, mediaInfo);
        if (minioProperties.getIsCompress()) src = processWithStrategy(src, COMPRESS_KEY, mediaInfo);
        return src;
    }

    /**
     * 调用指定策略处理，策略以 InputStream->InputStream 形式返回处理流
     */
    private byte[] processWithStrategy(byte[] src, String strategyKey, OssFileMediaInfo mediaInfo) throws Exception {
        ImageProcessingStrategy strategy = imageProcessingStrategyMap.get(strategyKey);
        if (strategy == null) return src;
        try (InputStream in = new ByteArrayInputStream(src);
             InputStream processed = strategy.process(in, mediaInfo);
             ByteArrayOutputStream os = new ByteArrayOutputStream(Math.max(4096, src.length / 2))) {
            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = processed.read(buf)) != -1) os.write(buf, 0, r);
            return os.toByteArray();
        }
    }

    /**
     * 上传字节数组到 MinIO（封装，抛 IOException）
     */
    private void uploadBytes(String bucket, String objectName, byte[] bytes, String contentType) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            minioUtils.uploadFile(bucket, objectName, is, contentType);
        }
    }

    /**
     * 小文件尽量读入内存返回 bytes；若大文件则返回 null（使用临时文件路径）
     */
    private byte[] tryReadToMemory(MultipartFile file) throws IOException {
        long size = file.getSize();
        if (size <= 0 || size > IN_MEMORY_THRESHOLD) return null;
        try (InputStream is = new BufferedInputStream(file.getInputStream(), IO_BUFFER);
             ByteArrayOutputStream os = new ByteArrayOutputStream((int) Math.max(1024, Math.min(size, 16 * 1024)))) {
            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            return os.toByteArray();
        }
    }

    /**
     * 将 MultipartFile 写到临时文件并返回文件引用（调用者负责删除）
     */
    private File toTempFile(MultipartFile file) throws IOException {
        File tmp = Files.createTempFile("oss_img_", "_" + Objects.requireNonNull(file.getOriginalFilename())).toFile();
        try (InputStream is = new BufferedInputStream(file.getInputStream(), IO_BUFFER);
             OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp), IO_BUFFER)) {
            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            os.flush();
        }
        return tmp;
    }

    /**
     * NSFW 检查：使用 supplier 提供流（注意 supplier 必须每次返回新的流）
     * 返回 ResponseEntity，body 中键 "valid" 为 true 表示违规
     */
    public ResponseEntity<?> checkImageUsingSupplier(Supplier<InputStream> supplier, long size, String filename) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            InputStreamResource resource = new InputStreamResource(supplier.get()) {
                @Override
                public String getFilename() {
                    return filename == null ? "file" : filename;
                }
                @Override
                public long contentLength() {
                    return size >= 0 && size <= Integer.MAX_VALUE ? size : -1;
                }
            };
            body.add("image", resource);
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(nsfwApiUrl, request, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(resp.getStatusCode()).body("分类接口请求失败");
            }
            Map<String, Double> result = resp.getBody();
            if (result == null || result.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "未收到有效结果"));
            }
            String[] violatingCategories = {"porn", "hentai", "sexy"};
            List<String> viol = Arrays.stream(violatingCategories)
                    .filter(result::containsKey)
                    .filter(k -> result.get(k) >= 0.5)
                    .map(k -> String.format("%s:%.2f", k, result.get(k)))
                    .toList();
            return ResponseEntity.ok(Map.of("valid", !viol.isEmpty(), "result", viol));
        } catch (Exception e) {
            log.error("checkImageUsingSupplier error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("校验异常: " + e.getMessage());
        }
    }

    /**
     * 生成外网访问地址（保留）
     */
    private FileVo persistAndReturn(OssFileImage doc) {
        OssFileImagePo entity = ossFileImageEntityMapper.toEntity(doc);
        ossFileImageRepository.save(entity);
        return fileVoMapper.toVo(entity);
    }
}
