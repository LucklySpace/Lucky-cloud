package com.xy.file.service;

import com.xy.general.response.domain.Result;
import org.springframework.web.multipart.MultipartFile;

public interface OssFileImageService {

    Result uploadImage(MultipartFile file);

    Result uploadAvatar(MultipartFile file);
}
