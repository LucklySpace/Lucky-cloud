package com.xy.lucky.file.service;


import com.xy.lucky.file.domain.OssFileUploadProgress;
import com.xy.lucky.file.domain.po.OssFilePo;
import com.xy.lucky.file.domain.vo.FileChunkVo;
import com.xy.lucky.file.domain.vo.FileVo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface OssFileService {

    /**
     * 获取分片上传进度
     * @param identifier 文件md5
     * @return 分片上传进度
     */
    OssFileUploadProgress getMultipartUploadProgress(String identifier);

    /**
     * 初始化分片上传任务
     * @param ossFilePo 文件信息
     * @return 响应结果
     */
    FileChunkVo initMultiPartUpload(OssFilePo ossFilePo);

    /**
     * 合并分片上传任务
     *
     * @param identifier 文件md5
     * @return 响应结果
     */
    FileVo mergeMultipartUpload(String identifier);

    /**
     * 判断文件是否存在
     * @param identifier 文件md5
     * @return 文件信息
     */
    FileVo isExits(String identifier);

    /**
     * 上传文件
     *
     * @param identifier 文件md5
     * @param file 文件
     * @return 响应结果
     */
    FileVo uploadFile(String identifier, MultipartFile file);

    /**
     * 分片下载
     *
     * @param identifier 文件md5
     * @param range      范围
     * @return 响应结果
     */
    ResponseEntity<?> downloadFile(String identifier, String range);


}
