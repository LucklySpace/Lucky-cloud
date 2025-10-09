package com.xy.file.service;

import com.xy.general.response.domain.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface OssFileImageService {

    /**
     * 上传图片
     */
    Result uploadImage(MultipartFile file);
}
