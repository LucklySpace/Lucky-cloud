package com.xy.lucky.file.service;


import com.xy.lucky.file.domain.OssFile;
import com.xy.lucky.file.util.ResponseResult;
import org.springframework.http.ResponseEntity;

public interface OssFileService {

    /**
     * 获取文件上传进度
     */
    ResponseResult getMultipartUploadProgress(String identifier);


    /**
     * 发起分片上传
     */
    ResponseResult initMultiPartUpload(OssFile ossFile);


    /**
     * 合并分片文件
     */
    ResponseResult mergeMultipartUpload(String identifier);


    /**
     * 检查文件是否存在
     */
    ResponseResult isExits(String identifier);

    /**
     * 下载文件
     */
    ResponseEntity downloadFile(String identifier, String range);

}