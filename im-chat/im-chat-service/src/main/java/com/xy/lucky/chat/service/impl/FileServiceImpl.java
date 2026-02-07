package com.xy.lucky.chat.service.impl;

import com.xy.lucky.general.exception.BusinessException;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.chat.domain.vo.FileVo;
import com.xy.lucky.chat.exception.FileException;
import com.xy.lucky.chat.service.FileService;
import com.xy.lucky.chat.utils.MinioUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
        Boolean exists = minioUtil.bucketExists(bucketName);
        if (Boolean.FALSE.equals(exists)) {
            Boolean created = minioUtil.makeBucket(bucketName);
            if (Boolean.TRUE.equals(created)) {
                minioUtil.setBucketPublic(bucketName);
            }
        }
    }

    @Override
    public FileVo uploadFile(FilePart file) {
        DataBuffer dataBuffer = DataBufferUtils.join(file.content()).block();
        if (dataBuffer == null) {
            throw new BusinessException(ResultCode.FAIL);
        }

        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);

        long size = bytes.length;
        if (size > 1000 * 1024 * 1024) {
            throw new BusinessException(ResultCode.REQUEST_DATA_TOO_LARGE);
        }

        String originalFilename = file.filename();
        String filePath = getFileType(originalFilename);
        MediaType contentType = file.headers().getContentType();
        String contentTypeStr = contentType != null ? contentType.toString() : "application/octet-stream";

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            String fileName = minioUtil.upload(bucketName, filePath, originalFilename, inputStream, size, contentTypeStr);
            if (StringUtils.isEmpty(fileName)) {
                throw new BusinessException(ResultCode.FAIL);
            }
            return new FileVo()
                    .setName(originalFilename)
                    .setPath(generUrl(filePath, fileName));
        } catch (Exception e) {
            throw new FileException("文件上传失败");
        }
    }

    @Override
    public FileVo uploadFile(File file) {
        if (file.length() > 1000 * 1024 * 1024) {
            throw new BusinessException(ResultCode.REQUEST_DATA_TOO_LARGE);
        }

        String originalFilename = file.getName();
        String filePath = getFileType(originalFilename);
        String contentType = "application/octet-stream";

        try (InputStream inputStream = new FileInputStream(file)) {
            String fileName = minioUtil.upload(bucketName, filePath, originalFilename, inputStream, file.length(), contentType);
            if (StringUtils.isEmpty(fileName)) {
                throw new BusinessException(ResultCode.FAIL);
            }
            return new FileVo()
                    .setName(originalFilename)
                    .setPath(generUrl(filePath, fileName));
        } catch (Exception e) {
            throw new FileException("文件上传失败");
        }
    }

    public String generUrl(String fileType, String fileName) {
        String url = minIOServer + "/" + bucketName + "/" + fileType + "/" + fileName;
        return url;
    }

}
