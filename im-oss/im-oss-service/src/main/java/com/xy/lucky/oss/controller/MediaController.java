package com.xy.lucky.oss.controller;

import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.service.OssFileImageService;
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
@Tag(name = "media", description = "媒体文件管理（S3）")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MediaController {

    @Resource
    private OssFileImageService ossImageFileService;

    @PostMapping("/image/upload")
    @Operation(summary = "上传图片文件", tags = {"media"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "file", description = "图片文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo uploadImage(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier, @RequestParam("file") MultipartFile file) {
        return ossImageFileService.uploadImage(identifier, file);
    }

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传头像文件", tags = {"media"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "file", description = "头像文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo uploadAvatar(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier, @RequestParam("file") MultipartFile file) {
        return ossImageFileService.uploadAvatar(identifier, file);
    }
}
