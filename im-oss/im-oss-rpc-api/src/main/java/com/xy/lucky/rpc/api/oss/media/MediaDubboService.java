package com.xy.lucky.rpc.api.oss.media;

import com.xy.lucky.rpc.api.oss.vo.FileVo;

/**
 * 媒体服务 Dubbo 接口
 */
public interface MediaDubboService {

    /**
     * 图片上传
     *
     * @param fileName    文件名
     * @param contentType 文件类型
     * @param fileBytes   图片文件字节数组
     * @return 上传结果
     */
    FileVo uploadImage(String fileName, String contentType, byte[] fileBytes);

    /**
     * 头像上传
     *
     * @param fileName    文件名
     * @param contentType 文件类型
     * @param fileBytes   头像文件字节数组
     * @return 上传结果
     */
    FileVo uploadAvatar(String fileName, String contentType, byte[] fileBytes);


    /**
     * 音频上传
     *
     * @param fileName    文件名
     * @param contentType 文件类型
     * @param fileBytes   音频文件字节数组
     * @return 上传结果
     */
    FileVo uploadAudio(String fileName, String contentType, byte[] fileBytes);
}
