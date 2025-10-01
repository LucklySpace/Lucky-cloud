package com.xy.file.controller;

import com.xy.file.service.OssFileImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/{version}/media")
@Tag(name = "media", description = "媒体文件管理")
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
    @Operation(summary = "上传图片文件", tags = {"media"}, description = "请使用此接口上传图片文件")
    @Parameters({
            @Parameter(name = "file", description = "图片文件", required = true, in = ParameterIn.DEFAULT)
    })
    public ResponseEntity uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("[文件上传] 图片文件处理");
        return ossImageFileService.uploadImage(file);
    }

}