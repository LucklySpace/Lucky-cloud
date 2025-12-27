package com.xy.lucky.server.controller;


import com.xy.lucky.domain.vo.FileVo;
import com.xy.lucky.server.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/{version}/file")
@Tag(name = "file", description = "文件")
public class FileController {

    @Resource
    private FileService fileService;

    @CrossOrigin(origins = "*") //重点
    @PostMapping(value = "/formUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", tags = {"file"}, description = "请使用此接口上传文件")
    @Parameters({
            @Parameter(name = "file", description = "文件对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<FileVo> uploadFile(@RequestPart("file") FilePart file) {
        return fileService.uploadFile(file);
    }


    // 文件上传接口
    @CrossOrigin(origins = "*") //重点
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<FileVo> upload(@RequestPart("file") FilePart file) {
        return fileService.uploadFile(file);
    }
}
