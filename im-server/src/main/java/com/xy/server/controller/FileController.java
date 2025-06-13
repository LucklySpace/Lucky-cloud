package com.xy.server.controller;


import com.xy.server.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

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
    public String uploadFile(@RequestPart("file") MultipartFile file) {
        return fileService.uploadFile(file);
    }


    // 文件上传接口
    @CrossOrigin(origins = "*") //重点
    @PostMapping("/upload")
    public String upload(MultipartFile file) {
        return fileService.uploadFile(file);
    }


    // 使用 InputStream 处理文件上传
    @PostMapping("/upload/stream")
    public ResponseEntity<String> uploadFileStream(InputStream inputStream) {
        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        // 可以通过请求头或其他方式获取文件名
        String fileName = "uploaded-file.txt";

        File outputFile = new File(uploadDir + fileName);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return new ResponseEntity<>("File uploaded successfully", HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Could not upload the file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
