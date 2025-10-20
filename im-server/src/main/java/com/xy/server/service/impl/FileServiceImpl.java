package com.xy.server.service.impl;

import com.xy.domain.vo.FileVo;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.exception.BusinessException;
import com.xy.server.service.FileService;
import com.xy.server.utils.MinioUtil;
import com.xy.server.utils.MockMultipartFile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        if (!minioUtil.bucketExists(bucketName)) {
            // 创建bucket
            minioUtil.makeBucket(bucketName);
            // 公开bucket
            minioUtil.setBucketPublic(bucketName);
        }
    }

    @Override
    public FileVo uploadFile(MultipartFile file) {

        // 大小校验
        if (file.getSize() > 1000 * 1024 * 1024) {
            throw new BusinessException(ResultCode.REQUEST_DATA_TOO_LARGE);
        }

        String filePath = getFileType(file.getOriginalFilename());

        // 上传
        String fileName = minioUtil.upload(bucketName, filePath, file);

        if (StringUtils.isEmpty(fileName)) {
            throw new BusinessException(ResultCode.FAIL);
        }

        FileVo fileVo = new FileVo()
                .setName(file.getOriginalFilename())
                .setPath(generUrl(filePath, fileName));

        return fileVo;

    }


    public String generUrl(String fileType, String fileName) {
        String url = minIOServer + "/" + bucketName + "/" + fileType + "/" + fileName;
        return url;
    }

    /**
     * file 转 MultipartFile
     *
     * @param file
     * @return
     */
    @Override
    public MultipartFile fileToMultipartFile(File file) {
        MultipartFile result = null;
        if (null != file) {
            try (FileInputStream input = new FileInputStream(file)) {
                result = new MockMultipartFile(file.getName(), file.getName(), "text/plain", input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }


    /**
     * file 转 MultipartFile
     *
     * @param file
     * @return
     */
    @Override
    public MultipartFile fileToImageMultipartFile(File file) {
        MultipartFile result = null;
        if (null != file) {
            try (FileInputStream input = new FileInputStream(file)) {
                result = new MockMultipartFile(file.getName(), file.getName(), "image/jpg", input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
