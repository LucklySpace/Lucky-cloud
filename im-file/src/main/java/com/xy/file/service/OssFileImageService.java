package com.xy.file.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface OssFileImageService {

    /**
     * 上传图片
     */
    ResponseEntity uploadImage(MultipartFile file);
}
