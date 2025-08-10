package com.xy.server.service;

import com.xy.domain.vo.FileVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileService {

    FileVo uploadFile(MultipartFile file);

    MultipartFile fileToMultipartFile(File file);

    MultipartFile fileToImageMultipartFile(File file);
}
