package com.xy.file.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * 上传文件
 */
@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OssFile {

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
     * 每个分片大小（byte）
     */
    private Long partSize;
    /**
     * 分片数量
     */
    private Integer partNum;
    /**
     * 是否已完成上传(完成合并),1是0否
     */
    private Integer isFinish;

    /**
     * 文件地址
     */
    private String path;

}