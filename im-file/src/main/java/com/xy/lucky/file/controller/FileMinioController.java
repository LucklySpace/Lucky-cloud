package com.xy.lucky.file.controller;

import com.xy.lucky.file.domain.OssFile;
import com.xy.lucky.file.domain.OssFileUploadProgress;
import com.xy.lucky.file.domain.vo.FileChunkVo;
import com.xy.lucky.file.domain.vo.FileVo;
import com.xy.lucky.file.service.OssFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * minio上传流程
 * <p>
 * 1.检查数据库中是否存在上传文件
 * <p>
 * 2.根据文件信息初始化，获取分片预签名url地址，前端根据url地址上传文件
 * <p>
 * 3.上传完成后，将分片上传的文件进行合并
 * <p>
 * 4.保存文件信息到数据库
 */
@Slf4j
@RestController
@RequestMapping("/api/{version}/file")
@Tag(name = "file", description = "文件管理")
public class FileMinioController {

    @Resource
    private OssFileService ossFileService;

    /**
     * 校验文件是否存在
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/check")
    @Operation(summary = "校验文件是否存在", tags = {"file"}, description = "请使用此接口检查文件是否存在")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY)
    })
    public OssFileUploadProgress getMultipartUploadProgress(@RequestParam("identifier") String identifier) {
        log.info("[文件校验] 检查文件是否存在，MD5: {}", identifier);
        return ossFileService.getMultipartUploadProgress(identifier);
    }

    /**
     * 分片初始化
     *
     * @param ossFile 文件信息
     */
    @PostMapping("/multipart/init")
    @Operation(summary = "分片初始化", tags = {"file"}, description = "请使用此接口初始化分片上传任务")
    @Parameters({
            @Parameter(name = "ossFile", description = "文件信息", required = true, in = ParameterIn.DEFAULT)
    })
    public FileChunkVo initMultiPartUpload(@RequestBody OssFile ossFile) {
        log.info("[分片初始化] 开始初始化分片上传任务，文件信息: {}", ossFile);
        return ossFileService.initMultiPartUpload(ossFile);
    }

    /**
     * 完成上传
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/merge")
    @Operation(summary = "完成上传", tags = {"file"}, description = "请使用此接口合并分片上传任务")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY)
    })
    public FileVo mergeMultiPartUpload(@RequestParam("identifier") String identifier) {
        log.info("[分片合并] 合并分片上传任务，MD5: {}", identifier);
        return ossFileService.mergeMultipartUpload(identifier);
    }

    /**
     * 判断文件是否存在
     *
     * @param identifier 文件md5
     */
    @GetMapping("/multipart/isExits")
    @Operation(summary = "判断文件是否存在", tags = {"file"}, description = "请使用此接口判断文件是否存在")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY)
    })
    public FileVo checkFileExists(@RequestParam("identifier") String identifier) {
        log.info("[文件检查] 判断文件是否存在，MD5: {}", identifier);
        return ossFileService.isExits(identifier);
    }

    /**
     * 上传
     *
     * @param identifier 文件md5
     * @param file       文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传", tags = {"file"}, description = "请使用此接口进行文件分片上传")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "file", description = "文件", required = true, in = ParameterIn.DEFAULT)
    })
    public FileVo uploadFile(@RequestParam("identifier") String identifier, @RequestParam("file") MultipartFile file) {
        log.info("[分片上传] 开始分片上传, 文件: {}", file.getOriginalFilename());
        return ossFileService.uploadFile(identifier, file);
    }

    /**
     * 分片下载
     *
     * @param identifier 文件md5
     * @param range      范围
     */
    @GetMapping("/download")
    @Operation(summary = "分片下载", tags = {"file"}, description = "请使用此接口进行文件分片下载")
    @Parameters({
            @Parameter(name = "identifier", description = "文件md5值", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "range", description = "下载范围", required = false, in = ParameterIn.HEADER)
    })
    public ResponseEntity<?> downloadFile(@RequestParam("identifier") String identifier,
                                       @RequestHeader(value = "Range", required = false) String range) {
        log.info("[文件下载] 开始文件分片下载，MD5: {}, Range: {}", identifier, range);
        return ossFileService.downloadFile(identifier, range);
    }

}
