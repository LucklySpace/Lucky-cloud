package com.xy.lucky.oss.service;


import com.xy.lucky.oss.domain.vo.FileVo;
import com.xy.lucky.oss.domain.vo.ImageVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片文件上传服务
 */
public interface OssFileImageService {

    /**
     * 图片上传
     *
     * @param identifier 文件md5
     * @param file       图片文件
     * @return 上传结果
     */
    ImageVo uploadImage(String identifier, MultipartFile file);

    /**
     * 头像上传
     *
     * @param identifier 文件md5
     * @param file       头像文件
     * @return 上传结果
     */
    ImageVo uploadAvatar(String identifier, MultipartFile file);

    /**
     * 获取上传文件地址
     *
     * @param identifier 文件md5
     * @return 上传地址
     */
    ImageVo getPresignedPutUrl(String identifier);

    /**
     * 音频上传
     *
     * @param identifier 文件md5
     * @param file       音频文件
     * @return 上传结果
     */
    FileVo uploadAudio(String identifier, MultipartFile file);
}
