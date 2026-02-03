package com.xy.lucky.oss.service;


import com.xy.lucky.oss.domain.vo.FileVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片文件上传服务
 */
public interface OssFileImageService {

    /**
     * 图片上传
     *
     * @param identifier 文件md5
     * @param file 图片文件
     * @return 上传结果
     */
    FileVo uploadImage(String identifier, MultipartFile file);

    /**
     * 头像上传
     *
     * @param identifier 文件md5
     * @param file 头像文件
     * @return 上传结果
     */
    FileVo uploadAvatar(String identifier, MultipartFile file);

}
