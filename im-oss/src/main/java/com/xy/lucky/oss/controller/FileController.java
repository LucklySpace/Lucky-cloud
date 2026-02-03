package com.xy.lucky.oss.controller;

import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.domain.vo.FileChunkVo;
import com.xy.lucky.oss.domain.vo.FileUploadProgressVo;
import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.service.OssFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/file", "/api/{version}/file"})
@Tag(name = "file", description = "文件管理（S3）")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    @Resource
    private OssFileService ossFileService;

    @GetMapping("/multipart/check")
    @Operation(summary = "校验文件是否存在", tags = {"file"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY)
    })
    public FileUploadProgressVo getMultipartUploadProgress(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier) {
        return ossFileService.getMultipartUploadProgress(identifier);
    }

    @PostMapping("/multipart/init")
    @Operation(summary = "分片初始化", tags = {"file"})
    @Parameters({
            @Parameter(name = "ossFile", description = "文件信息", required = true, in = ParameterIn.DEFAULT)
    })
    public FileChunkVo initMultiPartUpload(@RequestBody OssFilePo ossFilePo) {
        return ossFileService.initMultiPartUpload(ossFilePo);
    }

    @GetMapping("/multipart/merge")
    @Operation(summary = "完成上传", tags = {"file"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY)
    })
    public FileVo mergeMultiPartUpload(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier) {
        return ossFileService.mergeMultipartUpload(identifier);
    }

    @GetMapping("/multipart/isExits")
    @Operation(summary = "判断文件是否存在", tags = {"file"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY)
    })
    public FileVo checkFileExists(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier) {
        return ossFileService.isExits(identifier);
    }

    @PostMapping("/upload")
    @Operation(summary = "上传", tags = {"file"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "file", description = "文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo uploadFile(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier, @RequestParam("file") MultipartFile file) {
        return ossFileService.uploadFile(identifier, file);
    }

    @GetMapping("/download")
    @Operation(summary = "文件下载", tags = {"file"})
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "range", description = "下载范围", required = false, in = ParameterIn.HEADER)
    })
    public ResponseEntity<?> downloadFile(@NotBlank(message = "请输入文件md5值") @RequestParam("identifier") String identifier,
                                          @RequestHeader(value = "Range", required = false) String range) {
        return ossFileService.downloadFile(identifier, range);
    }

    @GetMapping("/md5")
    @Operation(summary = "获取文件md5值", tags = {"file"})
    @Parameters({
            @Parameter(name = "file", description = "文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo getFileMd5(@RequestParam("file") MultipartFile file) {
        return ossFileService.getFileMd5(file);
    }
}
