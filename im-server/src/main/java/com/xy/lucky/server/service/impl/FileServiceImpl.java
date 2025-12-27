package com.xy.lucky.server.service.impl;

import com.xy.lucky.domain.vo.FileVo;
import com.xy.lucky.general.exception.BusinessException;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.server.service.FileService;
import com.xy.lucky.server.utils.MinioUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileServiceImpl implements FileService {

    private static final Map<String, String> fileExtensions = new HashMap<>();

    static {
        // 图片类型
        fileExtensions.put("jpg", "image");
        fileExtensions.put("jpeg", "image");
        fileExtensions.put("png", "image");
        fileExtensions.put("gif", "image");
        fileExtensions.put("bmp", "image");

        // 音频类型
        fileExtensions.put("mp3", "audio");
        fileExtensions.put("wav", "audio");
        fileExtensions.put("ogg", "audio");
        fileExtensions.put("m4a", "audio");

        // 视频类型
        fileExtensions.put("mp4", "video");
        fileExtensions.put("avi", "video");
        fileExtensions.put("mkv", "video");
        fileExtensions.put("mov", "video");
        fileExtensions.put("flv", "video");

        // 文档类型
        fileExtensions.put("pdf", "document");
        fileExtensions.put("doc", "document");
        fileExtensions.put("docx", "document");
        fileExtensions.put("ppt", "document");
        fileExtensions.put("pptx", "document");
        fileExtensions.put("xls", "document");
        fileExtensions.put("xlsx", "document");
        fileExtensions.put("txt", "document");
    }

    @Value("${minio.public}")
    private String minIOServer;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Resource
    private MinioUtil minioUtil;

    public static String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1).toLowerCase();
            return fileExtensions.getOrDefault(extension, "file");
        }
        return "file";
    }

    @PostConstruct
    public void init() {
        // Initialization logic for Minio bucket
        minioUtil.bucketExists(bucketName)
                .flatMap(exists -> {
                    if (!exists) {
                        return minioUtil.makeBucket(bucketName)
                                .flatMap(success -> {
                                    if (success) {
                                        return minioUtil.setBucketPublic(bucketName);
                                    }
                                    return Mono.just(false);
                                });
                    }
                    return Mono.just(true);
                })
                .subscribe();
    }

    @Override
    public Mono<FileVo> uploadFile(FilePart file) {
        return DataBufferUtils.join(file.content())
                .publishOn(Schedulers.boundedElastic())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    InputStream inputStream = new java.io.ByteArrayInputStream(bytes);

                    // 大小校验
                    long size = bytes.length;
                    if (size > 1000 * 1024 * 1024) {
                        return Mono.error(new BusinessException(ResultCode.REQUEST_DATA_TOO_LARGE));
                    }

                    String originalFilename = file.filename();
                    String filePath = getFileType(originalFilename);
                    MediaType contentType = file.headers().getContentType();
                    String contentTypeStr = contentType != null ? contentType.toString() : "application/octet-stream";

                    // 上传
                    return minioUtil.upload(bucketName, filePath, originalFilename, inputStream, size, contentTypeStr)
                            .flatMap(fileName -> {
                                if (StringUtils.isEmpty(fileName)) {
                                    return Mono.error(new BusinessException(ResultCode.FAIL));
                                }
                                FileVo fileVo = new FileVo()
                                        .setName(originalFilename)
                                        .setPath(generUrl(filePath, fileName));
                                return Mono.just(fileVo);
                            });
                });
    }

    @Override
    public Mono<FileVo> uploadFile(File file) {
        // 大小校验
        if (file.length() > 1000 * 1024 * 1024) {
            return Mono.error(new BusinessException(ResultCode.REQUEST_DATA_TOO_LARGE));
        }

        String originalFilename = file.getName();
        String filePath = getFileType(originalFilename);
        String contentType = "application/octet-stream"; // Default or detect

        return Mono.fromCallable(() -> new FileInputStream(file))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(inputStream ->
                        minioUtil.upload(bucketName, filePath, originalFilename, inputStream, file.length(), contentType)
                                .doFinally(signal -> {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                        // ignore
                                    }
                                })
                )
                .flatMap(fileName -> {
                    if (StringUtils.isEmpty(fileName)) {
                        return Mono.error(new BusinessException(ResultCode.FAIL));
                    }
                    return Mono.just(new FileVo()
                            .setName(originalFilename)
                            .setPath(generUrl(filePath, fileName)));
                });
    }

    public String generUrl(String fileType, String fileName) {
        String url = minIOServer + "/" + bucketName + "/" + fileType + "/" + fileName;
        return url;
    }

}
