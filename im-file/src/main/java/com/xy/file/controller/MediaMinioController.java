package com.xy.file.controller;

import com.xy.file.service.OssFileImageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/media")
public class MediaMinioController {


    @Resource
    private OssFileImageService ossImageFileService;

    /**
     * 上传接口
     *
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/image")
    public ResponseEntity uploadImage(@RequestParam MultipartFile file) {
        log.info("[文件上传] 图片文件处理");
        return ossImageFileService.uploadImage(file);
    }

}
