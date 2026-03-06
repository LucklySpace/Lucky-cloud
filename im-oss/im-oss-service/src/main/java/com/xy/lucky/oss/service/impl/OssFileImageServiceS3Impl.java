package com.xy.lucky.oss.service.impl;

import com.xy.lucky.oss.client.OssProperties;
import com.xy.lucky.oss.domain.OssFileMediaInfo;
import com.xy.lucky.oss.domain.mapper.FileVoMapper;
import com.xy.lucky.oss.domain.po.OssFileImagePo;
import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.domain.vo.ImageVo;
import com.xy.lucky.oss.enums.StorageBucketEnum;
import com.xy.lucky.oss.exception.FileException;
import com.xy.lucky.oss.handler.ImageProcessingStrategy;
import com.xy.lucky.oss.repository.OssFileImageRepository;
import com.xy.lucky.oss.repository.OssFileRepository;
import com.xy.lucky.oss.service.OssFileImageService;
import com.xy.lucky.oss.util.MD5Utils;
import com.xy.lucky.oss.util.OssUtils;
import com.xy.lucky.oss.util.RedisUtils;
import com.xy.lucky.utils.id.IdUtils;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Slf4j
@Service
public class OssFileImageServiceS3Impl implements OssFileImageService {

    private static final String THUMBNAIL_KEY = "thumbnail";
    private static final String WATERMARK_KEY = "watermark";
    private static final String COMPRESS_KEY = "compress";
    // 缩略图后缀
    private static final String THUMBNAIL_PREFIX = ".thumb";

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
    private OssFileRepository ossFileRepository;
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
    public ImageVo uploadImage(String identifier, MultipartFile file) {
        OssFileImagePo ossFileByIdentifier = precheckUpload(identifier, file);
        if (Objects.nonNull(ossFileByIdentifier)) {
            return fileVoMapper.toVo(ossFileByIdentifier);
        }

        String originalFilename = file.getOriginalFilename();
        String bucket = ossUtils.getOrCreateBucketByImage();
        String objectName = buildObjectName(originalFilename);
        String fileSuffix = ossUtils.getFileSuffix(originalFilename);

        File tmp = null;
        byte[] memBytes = null;
        Supplier<InputStream> supplier;
        try {
            memBytes = tryReadToMemory(file);
            if (memBytes != null) {
                byte[] bytesRef = memBytes;
                supplier = () -> new ByteArrayInputStream(bytesRef);
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
            byte[] cachedBytes = memBytes;
            CompletableFuture<Void> mainProcessed = CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] base = cachedBytes != null ? cachedBytes : toBytes(supplier);
                    return applyStrategies(base, mediaInfo);
                } catch (Exception ex) {
                    log.error("主图处理失败: {}", ex.getMessage(), ex);
                    throw new FileException("主图处理失败: " + ex.getMessage());
                }
            }, executor).thenAcceptAsync(bytes -> {
                try {
                    uploadBytes(bucket, objectName, bytes, "image/png");
                } catch (IOException e) {
                    throw new FileException("主图上传失败: " + e.getMessage());
                }
            }, executor);

            CompletableFuture.allOf(mainProcessed).join();

            OssFileImagePo reqOssImagePo = buildImagePo(identifier, file, bucket, objectName, fileSuffix);

            if (Boolean.TRUE.equals(ossProperties.getCreateThumbnail())) {
                CompletableFuture.runAsync(() -> {
                    try {
                        byte[] base = cachedBytes != null ? cachedBytes : toBytes(supplier);
                        byte[] thumb = processWithStrategy(base, THUMBNAIL_KEY, mediaInfo);
                        String thumbName = objectName + THUMBNAIL_PREFIX;
                        uploadBytes(bucket, thumbName, thumb, "image/png");
                    } catch (Exception e) {
                        log.error("缩略图处理失败: " + e.getMessage());
                    }
                }, executor);
                reqOssImagePo.setHasThumbnail(true);
            }
            saveOssImageToRedis(reqOssImagePo);
            return persistAndReturn(reqOssImagePo);
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

    private void saveOssImageToRedis(OssFileImagePo ossImagePo) {
        redisUtils.set(ossImagePo.getIdentifier(), ossImagePo, 30 * 60);
    }

    @Override
    public ImageVo uploadAvatar(String identifier, MultipartFile file) {
        OssFileImagePo ossFileByIdentifier = precheckUpload(identifier, file);
        if (Objects.nonNull(ossFileByIdentifier)) {
            return fileVoMapper.toVo(ossFileByIdentifier);
        }

        String originalFilename = file.getOriginalFilename();
        String fileSuffix = ossUtils.getFileSuffix(originalFilename);

        try {
            String bucket = ossUtils.getOrCreateBucketByAvatar();
            if (!avatarBucketIsPublic.get()) {
                try {
                    if (ossUtils.setBucketPublic(bucket)) avatarBucketIsPublic.set(true);
                } catch (Exception ignored) {
                    log.warn("设置头像bucket为公开失败");
                }
            }

            String objectName = buildObjectName(file.getOriginalFilename());

            try (InputStream is = file.getInputStream()) {
                ossUtils.uploadFile(bucket, objectName, is, file.getContentType());
            }

            OssFileImagePo doc = buildImagePo(identifier, file, bucket, objectName, fileSuffix);

            return persistAndReturn(doc);
        } catch (Exception e) {
            throw new FileException("头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileVo uploadAudio(String identifier, MultipartFile file) {
        validateUploadInput(identifier, file);
        // TODO 音频上传  后续需实现语音文件格式转换
        try {
            String bucket = ossUtils.getOrCreateBucketByAudio();

            String objectName = buildObjectName(file.getOriginalFilename());

            try (InputStream is = file.getInputStream()) {
                ossUtils.uploadFile(bucket, objectName, is, file.getContentType());
            }

            OssFilePo doc = buildFilePo(identifier, file, bucket, objectName,
                    ossUtils.getPresignedUrl(bucket, objectName, ossProperties.getPresignedUrlExpiry()));

            return persistAndReturn(doc);

        } catch (Exception e) {
            throw new FileException("音频上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件字节数组
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
     * 处理图片
     */
    private byte[] applyStrategies(byte[] src, OssFileMediaInfo mediaInfo) throws Exception {
        if (Boolean.TRUE.equals(ossProperties.getCreateWatermark()))
            src = processWithStrategy(src, WATERMARK_KEY, mediaInfo);
        if (Boolean.TRUE.equals(ossProperties.getCompress())) src = processWithStrategy(src, COMPRESS_KEY, mediaInfo);
        return src;
    }

    /**
     * 处理图片
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
     * 上传文件
     */
    private void uploadBytes(String bucket, String objectName, byte[] bytes, String contentType) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            ossUtils.uploadFile(bucket, objectName, is, contentType);
        }
    }

    /**
     * 尝试将文件读取到内存中
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
     * 将文件转为临时文件
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
     * 检查图片
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("校验异常: " + e.getMessage());
        }
    }

    @Override
    public ImageVo getPresignedPutUrl(String identifier) {
        OssFileImagePo file = getOssFileByIdentifier(identifier);

        String path = ossUtils.getPresignedUrl(file.getBucketName(), file.getObjectKey(), ossProperties.getPresignedUrlExpiry());
        String thumbPath = file.getHasThumbnail() ?
                ossUtils.getPresignedUrl(file.getBucketName(), file.getObjectKey() + THUMBNAIL_PREFIX, ossProperties.getPresignedUrlExpiry()) : path;

        return ImageVo.builder()
                .key(identifier)
                .path(path)
                .thumbPath(thumbPath)
                .build();
    }

    /**
     * 保存文件信息
     */
    private ImageVo persistAndReturn(OssFileImagePo doc) {
        OssFileImagePo entity = ossFileImageRepository.save(doc);
        return fileVoMapper.toVo(entity);
    }

    /**
     * 保存文件信息
     */
    private FileVo persistAndReturn(OssFilePo doc) {
        OssFilePo entity = ossFileRepository.save(doc);
        return fileVoMapper.toVo(entity);
    }

    /**
     * 获取文件信息
     */
    private OssFileImagePo precheckUpload(String identifier, MultipartFile file) {
        validateUploadInput(identifier, file);
        return getOssFileByIdentifier(identifier);
    }

    private void validateUploadInput(String identifier, MultipartFile file) {
        String originalFilename = file == null ? null : file.getOriginalFilename();
        if (file == null || file.isEmpty() || !StringUtils.hasText(identifier)) {
            throw new FileException("文件或文件md5不能为空");
        }
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new FileException("文件名格式错误");
        }
        MD5Utils.checkMD5(identifier, file);
    }

    /**
     * 获取文件
     */
    private String buildObjectName(String originalFilename) {
        return ossUtils.getObjectName(ossUtils.generatePath(), IdUtils.randomUUID());
    }

    /**
     * 构建文件信息
     */
    private OssFileImagePo buildImagePo(String identifier, MultipartFile file, String bucket, String objectName, String suffix) {
        String originalFilename = file.getOriginalFilename();
        return OssFileImagePo
                .builder().
                identifier(identifier)
                .bucketName(bucket)
                .fileName(originalFilename)
                .objectKey(objectName)
                .fileType(StorageBucketEnum.getBucketCodeByFilename(originalFilename))
                .contentType(file.getContentType())
                .suffix(suffix)
                .fileSize(file.getSize())
                .build();
    }


    /**
     * 构建文件信息
     */
    private OssFilePo buildFilePo(String identifier, MultipartFile file, String bucket, String objectName, String path) {
        String originalFilename = file.getOriginalFilename();
        return OssFilePo
                .builder().
                identifier(identifier)
                .bucketName(bucket)
                .fileName(originalFilename)
                .objectKey(objectName)
                .fileType(StorageBucketEnum.getBucketCodeByFilename(originalFilename))
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .build();
    }

    /**
     * 获取文件
     */
    private OssFileImagePo getOssFileByIdentifier(String identifier) {
        return Optional.ofNullable((OssFileImagePo) redisUtils.get(identifier)).orElse(findFromDb(identifier));
    }

    /**
     * 获取文件
     */
    private OssFileImagePo findFromDb(String identifier) {
        return ossFileImageRepository.findByIdentifier(identifier).orElseThrow(() -> new FileException("文件不存在"));
    }
}
