package com.xy.server.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface FileService {

    String uploadFile(MultipartFile file);

    MultipartFile fileToMultipartFile(File file);

    MultipartFile fileToImageMultipartFile(File file);
}
