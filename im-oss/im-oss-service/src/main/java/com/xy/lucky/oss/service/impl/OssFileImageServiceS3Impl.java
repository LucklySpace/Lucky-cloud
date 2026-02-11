package com.xy.lucky.oss.service.impl;

import com.xy.lucky.oss.client.OssProperties;
import com.xy.lucky.oss.domain.OssFileMediaInfo;
import com.xy.lucky.oss.domain.po.OssFileImagePo;
import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.enums.StorageBucketEnum;
import com.xy.lucky.oss.exception.FileException;
import com.xy.lucky.oss.handler.ImageProcessingStrategy;
import com.xy.lucky.oss.mapper.FileVoMapper;
import com.xy.lucky.oss.repository.OssFileImageRepository;
import com.xy.lucky.oss.service.OssFileImageService;
import com.xy.lucky.oss.util.MD5Utils;
import com.xy.lucky.oss.util.OssUtils;
import com.xy.lucky.oss.util.RedisUtils;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.json.JacksonUtils;
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
import org.springframework.util.StringUtils;
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

@Slf4j
@Service
public class OssFileImageServiceS3Impl implements OssFileImageService {

    private static final String THUMBNAIL_KEY = "thumbnail";
    private static final String WATERMARK_KEY = "watermark";
    private static final String COMPRESS_KEY = "compress";

    private static final long IN_MEMORY_THRESHOLD = 5 * 1024 * 1024L;
    private static final int IO_BUFFER = 16 * 1024;

    private static final AtomicBoolean avatarBucketIsPublic = new AtomicBoolean(false);

    @Resource
    private OssUtils ossUtils;
    @Resource
    private OssProperties ossProperties;
    @Resource
    private OssFileImageRepository ossFileImageRepository;
    @Resource
    private FileVoMapper fileVoMapper;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisUtils redisUtils;
    @Resource(name = "asyncServiceExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private Map<String, ImageProcessingStrategy> imageProcessingStrategyMap;

    @Value("${nsfw.api.url:http://localhost:3000/classify}")
    private String nsfwApiUrl;

    @Override
    public FileVo uploadImage(String identifier, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (file == null || file.isEmpty() || !StringUtils.hasText(identifier)) {
            throw new FileException("文件或文件md5不能为空");
        }
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new FileException("文件名格式错误");
        }
        MD5Utils.checkMD5(identifier, file);

        OssFileImagePo ossFileByIdentifier = getOssFileByIdentifier(identifier);
        if (Objects.nonNull(ossFileByIdentifier)) {
            return fileVoMapper.toVo(ossFileByIdentifier);
        }

        String bucket = ossUtils.getOrCreateBucketByFileName(file.getOriginalFilename());
        String fileName = IdUtils.randomUUID() + "." + StorageBucketEnum.getSuffix(Objects.requireNonNull(file.getOriginalFilename()));
        String objectName = ossUtils.getObjectName(ossUtils.generatePath(), fileName);

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

            if (Boolean.TRUE.equals(ossProperties.getCheckFile())) {
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

            OssFileMediaInfo mediaInfo = new OssFileMediaInfo();

            final Supplier<InputStream> mainSupplier = supplier;
            CompletableFuture<byte[]> mainProcessed = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] base = toBytes(mainSupplier);
                    return applyStrategies(base, mediaInfo);
                } catch (Exception ex) {
                    throw new FileException("主图处理失败: " + ex.getMessage());
                }
            }, executor);

            CompletableFuture<String> thumbFuture = CompletableFuture.completedFuture(null);
            if (Boolean.TRUE.equals(ossProperties.getCreateThumbnail())) {
                final Supplier<InputStream> thumbSupplier = supplier;
                thumbFuture = CompletableFuture.supplyAsync(() -> {
                    try {
                        byte[] base = toBytes(thumbSupplier);
                        byte[] thumb = processWithStrategy(base, THUMBNAIL_KEY, mediaInfo);
                        String thumbName = objectName + ".thumb.png";
                        uploadBytes(bucket, thumbName, thumb, "image/png");
                        return ossUtils.getFilePath(bucket, thumbName);
                    } catch (Exception e) {
                        return null;
                    }
                }, executor);
            }

            CompletableFuture<String> mainUpload = mainProcessed.thenApplyAsync(bytes -> {
                try {
                    uploadBytes(bucket, objectName, bytes, "image/png");
                    return ossUtils.getFilePath(bucket, objectName);
                } catch (IOException e) {
                    throw new FileException("主图上传失败: " + e.getMessage());
                }
            }, executor);

            CompletableFuture.allOf(mainUpload, thumbFuture).join();

            String mainPath = mainUpload.join();
            String thumbPath = thumbFuture.join();

            OssFileImagePo doc = OssFileImagePo
                    .builder().
                    identifier(identifier)
                    .bucketName(bucket)
                    .fileName(file.getOriginalFilename())
                    .objectKey(objectName)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .path(mainPath).build();

            if (thumbPath != null) {
                doc.setThumbnailPath(thumbPath);
            }
            return persistAndReturn(doc);
        } catch (Exception ex) {
            throw new FileException("图片上传失败: " + ex.getMessage());
        } finally {
            if (tmp != null && tmp.exists()) {
                File t = tmp;
                CompletableFuture.runAsync(() -> {
                    try {
                        Files.deleteIfExists(t.toPath());
                    } catch (Exception ignored) {
                    }
                }, executor);
            }
        }
    }

    @Override
    public FileVo uploadAvatar(String identifier, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (file == null || file.isEmpty() || !StringUtils.hasText(identifier)) {
            throw new FileException("文件或文件md5不能为空");
        }
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new FileException("文件名格式错误");
        }
        MD5Utils.checkMD5(identifier, file);

        OssFileImagePo ossFileByIdentifier = getOssFileByIdentifier(identifier);
        if (Objects.nonNull(ossFileByIdentifier)) {
            return fileVoMapper.toVo(ossFileByIdentifier);
        }

        try {
            String bucket = ossUtils.getOrCreateBucketByAvatar();
            if (!avatarBucketIsPublic.get()) {
                try {
                    if (ossUtils.setBucketPublic(bucket)) avatarBucketIsPublic.set(true);
                } catch (Exception ignored) {
                }
            }

            String fileName = IdUtils.randomUUID() + "." + StorageBucketEnum.getSuffix(Objects.requireNonNull(file.getOriginalFilename()));
            String objectName = ossUtils.getObjectName(ossUtils.generatePath(), fileName);

            try (InputStream is = file.getInputStream()) {
                ossUtils.uploadFile(bucket, objectName, is, file.getContentType());
            }

            OssFileImagePo doc = OssFileImagePo
                    .builder().
                    identifier(identifier)
                    .bucketName(bucket)
                    .fileName(file.getOriginalFilename())
                    .objectKey(objectName)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .path(ossUtils.getPublicFilePath(bucket, objectName)).build();

            return persistAndReturn(doc);
        } catch (Exception e) {
            throw new FileException("头像上传失败: " + e.getMessage());
        }
    }

    private byte[] toBytes(Supplier<InputStream> supplier) throws IOException {
        try (InputStream is = supplier.get();
             ByteArrayOutputStream os = new ByteArrayOutputStream(Math.max(4096, IO_BUFFER))) {
            byte[] buf = new byte[IO_BUFFER];
            int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
            return os.toByteArray();
        }
    }

    private byte[] applyStrategies(byte[] src, OssFileMediaInfo mediaInfo) throws Exception {
        if (Boolean.TRUE.equals(ossProperties.getCreateWatermark()))
            src = processWithStrategy(src, WATERMARK_KEY, mediaInfo);
        if (Boolean.TRUE.equals(ossProperties.getCompress())) src = processWithStrategy(src, COMPRESS_KEY, mediaInfo);
        return src;
    }

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

    private void uploadBytes(String bucket, String objectName, byte[] bytes, String contentType) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            ossUtils.uploadFile(bucket, objectName, is, contentType);
        }
    }

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("校验异常: " + e.getMessage());
        }
    }

    private FileVo persistAndReturn(OssFileImagePo doc) {
        OssFileImagePo entity = ossFileImageRepository.save(doc);
        return fileVoMapper.toVo(entity);
    }

    private OssFileImagePo getOssFileByIdentifier(String identifier) {
        String objStr = redisUtils.get(identifier);
        if (!StringUtils.hasText(objStr)) {
            return findFromDb(identifier);
        }
        return JacksonUtils.parseObject(objStr, OssFileImagePo.class);
    }

    private OssFileImagePo findFromDb(String identifier) {
        return ossFileImageRepository.findByIdentifier(identifier).orElse(null);
    }
}
