package com.xy.lucky.rpc.api.oss.file;

import com.xy.lucky.rpc.api.oss.dto.FileDownloadRangeDto;
import com.xy.lucky.rpc.api.oss.dto.OssFileDto;
import com.xy.lucky.rpc.api.oss.vo.FileChunkVo;
import com.xy.lucky.rpc.api.oss.vo.FileUploadProgressVo;
import com.xy.lucky.rpc.api.oss.vo.FileVo;

/**
 * 文件服务 Dubbo 接口
 */
public interface FileDubboService {

    /**
     * 获取分片上传进度
     *
     * @param identifier 文件md5
     * @return 分片上传进度
     */
    FileUploadProgressVo getMultipartUploadProgress(String identifier);

    /**
     * 初始化分片上传任务
     *
     * @param ossFileDto 文件信息
     * @return 响应结果
     */
    FileChunkVo initMultiPartUpload(OssFileDto ossFileDto);

    /**
     * 合并分片上传任务
     *
     * @param identifier 文件md5
     * @return 响应结果
     */
    FileVo mergeMultipartUpload(String identifier);

    /**
     * 判断文件是否存在
     *
     * @param identifier 文件md5
     * @return 文件信息
     */
    FileVo isExits(String identifier);

    /**
     * 上传文件
     *
     * @param identifier  文件md5
     * @param fileName    文件名
     * @param contentType 文件类型
     * @param fileBytes   文件字节数组
     * @return 响应结果
     */
    FileVo uploadFile(String identifier, String fileName, String contentType, byte[] fileBytes);

    /**
     * 分片下载
     *
     * @param identifier 文件md5
     * @param range      范围字符串，格式：bytes=start-end
     * @return 下载范围信息
     */
    FileDownloadRangeDto downloadFile(String identifier, String range);

    /**
     * 获取文件md5
     *
     * @param fileBytes 文件字节数组
     * @param fileName  文件名
     * @return 文件md5
     */
    FileVo getFileMd5(byte[] fileBytes, String fileName);
}
