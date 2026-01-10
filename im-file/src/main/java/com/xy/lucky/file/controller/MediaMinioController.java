package com.xy.lucky.file.controller;

import com.xy.lucky.file.domain.vo.FileVo;
import com.xy.lucky.file.service.OssFileImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/media", "/api/{version}/media"})
@Tag(name = "media", description = "媒体文件管理")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MediaMinioController {

    @Resource
    private OssFileImageService ossImageFileService;

    @PostMapping("/image/upload")
    @Operation(summary = "上传图片文件", tags = {"media"}, description = "请使用此接口上传图片文件, 此接口使用时必须计算文件md5, 用于校验文件完整性")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "file", description = "图片文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo uploadImage(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier, @RequestParam("file") MultipartFile file) {
        log.info("[图片上传] 开始上传图片文件");
        return ossImageFileService.uploadImage(identifier, file);
    }

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像文件", tags = {"media"}, description = "请使用此接口上传头像文件, 此接口使用时必须计算文件md5, 用于校验文件完整性")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "file", description = "头像文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo uploadAvatar(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier, @RequestParam("file") MultipartFile file) {
        log.info("[头像上传] 开始上传头像文件");
        return ossImageFileService.uploadAvatar(identifier, file);
    }

}
