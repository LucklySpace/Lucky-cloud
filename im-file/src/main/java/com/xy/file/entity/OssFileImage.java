package com.xy.file.entity;

import lombok.*;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OssFileImage {

    /**
     * 主键
     */
    private String id;
    /**
     * 分片上传的uploadId
     */
    private String uploadId;
    /**
     * 桶名称
     */
    private String bucketName;
    /**
     * 文件唯一标识（md5）
     */
    private String identifier;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件类型
     */
    private String fileType;
    /**
     * 文件的key
     */
    private String objectKey;

    private String contentType;
    /**
     * 文件大小（byte）
     */
    private Long fileSize;
    /**
     * 文件地址
     */
    private String path;
    /**
     * 缩略图文件地址
     */
    private String thumbnailPath;


}
