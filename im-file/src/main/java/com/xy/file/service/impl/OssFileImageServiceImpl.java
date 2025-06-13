package com.xy.file.service.impl;

import com.xy.file.client.MinioProperties;
import com.xy.file.entity.OssFileImage;
import com.xy.file.entity.OssFileMediaInfo;
import com.xy.file.handler.ImageProcessingStrategy;
import com.xy.file.handler.impl.CompressStrategy;
import com.xy.file.handler.impl.ThumbnailStrategy;
import com.xy.file.handler.impl.WatermarkStrategy;
import com.xy.file.service.OssFileImageService;
import com.xy.file.util.MinioUtils;
import com.xy.file.util.RedisRepo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
public class OssFileImageServiceImpl implements OssFileImageService {

    // 缩略图
    private final String THUMBNAIL_IMAGE = "thumbnail";
    // 水印
    private final String WATERMARK_IMAGE = "watermark";
    // 图片压缩
    private final String COMPRESS_IMAGE = "compress";
    private final Map<String, ImageProcessingStrategy> strategies = new HashMap<>();

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private RedisRepo redisRepo;

    @Resource
    private MinioProperties minioProperties;

    @Value("${nsfw.api.url:http://localhost:3000/classify}") // 分类接口 URL
    private String nsfwApiUrl;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 多线程协程
     */
    @Resource(name = "asyncServiceExecutor")
    private ThreadPoolTaskExecutor applicationTaskExecutor;

    public OssFileImageServiceImpl() {
        this.strategies.put(THUMBNAIL_IMAGE, new ThumbnailStrategy());
        this.strategies.put(WATERMARK_IMAGE, new WatermarkStrategy());
        this.strategies.put(COMPRESS_IMAGE, new CompressStrategy());
    }

    public static File multipartFileToFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            throw new IOException("传入的MultipartFile为空");
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String tempFileSuffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : ".tmp";
        File tempFile = File.createTempFile("temp", tempFileSuffix);
        try (InputStream ins = multipartFile.getInputStream();
             OutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = ins.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    /**
     * 上传图片
     *
     * @param file
     * @return
     */
    @Override
    public ResponseEntity<?> uploadImage(MultipartFile file) {
        try {
            OssFileImage ossFile = new OssFileImage();
            OssFileImage processedFile = imageStrategy(file, ossFile);
            log.info("图片上传成功: 文件名 {} ", file.getOriginalFilename());
            return ResponseEntity.ok(processedFile);
        } catch (Exception e) {
            log.error("图片上传失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("图片上传失败");
        }
    }

    /**
     * 图片处理
     *
     * @param file    上传的文件
     * @param ossFile OSS文件信息
     * @return 处理后的OSS文件信息
     * @throws Exception 异常
     */
    public OssFileImage imageStrategy(MultipartFile file, OssFileImage ossFile) throws Exception {
        byte[] fileBytes = file.getInputStream().readAllBytes(); // 将文件流一次性读取到内存中
        OssFileMediaInfo ossMediaFileInfo = new OssFileMediaInfo();

        // 自动生成路径和文件名
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), file.getOriginalFilename());

        String fileType = StringUtils.hasText(ossFile.getFileType()) ? ossFile.getFileType() : file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);

        String bucketName = minioUtils.getOrCreateBucketByFileType("image");

        if (minioProperties.getIsChecked()) {
            CompletableFuture<Void> checkImageFuture = CompletableFuture.runAsync(() -> {
                // 用户性别
                ResponseEntity res = checkImage(file, 0.5);
                log.info("checkImage result:{}", res.getBody().toString());
            }, applicationTaskExecutor);
            checkImageFuture.get();
        }

        // 处理缩略图
        if (minioProperties.getCreateThumbnail()) {
            processImage(fileBytes, ossMediaFileInfo, THUMBNAIL_IMAGE, THUMBNAIL_IMAGE, objectName, ossFile::setThumbnailPath);
        }

        // 处理水印
        if (minioProperties.getCreateWatermark()) {
            fileBytes = processImage(fileBytes, ossMediaFileInfo, WATERMARK_IMAGE);
        }

        // 处理压缩
        if (minioProperties.getIsCompress()) {
            fileBytes = processImage(fileBytes, ossMediaFileInfo, COMPRESS_IMAGE);
        }

        // 上传主图
        try (InputStream mainImageStream = new ByteArrayInputStream(fileBytes)) {
            minioUtils.uploadFile(bucketName, objectName, mainImageStream, "image/png");
            ossFile.setPath(minioUtils.getFilePath(bucketName, objectName));
        }

        return ossFile;
    }

    /**
     * 通用图片处理逻辑
     *
     * @param fileBytes        图片的字节数组
     * @param ossMediaFileInfo OSS 文件信息
     * @param strategyKey      策略 Key（缩略图、水印或压缩）
     * @return 处理后的图片字节数组
     * @throws Exception 异常
     */
    private byte[] processImage(byte[] fileBytes, OssFileMediaInfo ossMediaFileInfo, String strategyKey) throws Exception {
        ImageProcessingStrategy strategy = strategies.get(strategyKey);
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            InputStream processedStream = strategy.process(inputStream, ossMediaFileInfo);
            processedStream.transferTo(outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 处理图片并上传到 MinIO
     *
     * @param fileBytes        图片的字节数组
     * @param ossMediaFileInfo OSS 文件信息
     * @param strategyKey      策略 Key
     * @param bucketType       文件桶类型
     * @param objectName       文件名
     * @param pathSetter       用于设置路径的方法引用
     * @throws Exception 异常
     */
    private void processImage(byte[] fileBytes, OssFileMediaInfo ossMediaFileInfo, String strategyKey, String bucketType,
                              String objectName, Consumer<String> pathSetter) throws Exception {
        ImageProcessingStrategy strategy = strategies.get(strategyKey);
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            InputStream processedStream = strategy.process(inputStream, ossMediaFileInfo);

            String bucketName = minioUtils.getOrCreateBucketByFileType(bucketType);
            minioUtils.uploadFile(bucketName, objectName, processedStream, "image/png");

            String filePath = minioUtils.getFilePath(bucketName, objectName);
            pathSetter.accept(filePath);
        }
    }

    /**
     * 检查图片
     *
     * @param file
     * @param threshold
     * @return
     */
    public ResponseEntity checkImage(MultipartFile file, Double threshold) {
        try {
            // 读取本地文件并构建请求体
            File tempFile = multipartFileToFile(file);
            if (!tempFile.exists()) {
                return ResponseEntity.badRequest().body("文件路径无效或文件不存在！");
            }

            // 构建 multipart/form-data 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new FileSystemResource(tempFile));

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
                if (result.containsKey(category) && result.get(category) >= threshold) {
                    violations.add(String.format("类别: %s, 概率: %.2f", category, result.get(category)));
                }
            }

            if (!violations.isEmpty()) {
                return ResponseEntity.ok(Map.of("valid", true, "result", violations));
            }

            return ResponseEntity.ok(Map.of("valid", false));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("服务器异常: " + e.getMessage());
        }
    }

}
