package com.xy.file.entity;

import lombok.*;

import java.util.Map;

/**
 * 文件上传进度响应对象
 */
@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OssFileUploadProgress {

    /**
     * 是否为新文件（从未上传过的文件）,1是0否
     */
    private Integer isNew;

    /**
     * 是否已完成上传（是否已经合并分片）,1是0否
     */
    private Integer isFinish;

    /**
     * 文件地址
     */
    private String path;

    /**
     * 上传id
     */
    private String uploadId;

    /**
     * 未完全上传时,还未上传的(分片、上传链接)Map
     */
    private Map<String, String> undoneChunkMap;

}
