package com.xy.lucky.file.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "图片文件")
public class OssFileImage {

    @Schema(description = "主键")
    private String id;

    @Schema(description = "分片上传的uploadId")
    private String uploadId;

    @Schema(description = "桶名称")
    private String bucketName;

    @Schema(description = "文件唯一标识（md5）")
    private String identifier;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件的key")
    private String objectKey;

    @Schema(description = "文件内容类型")
    private String contentType;

    @Schema(description = "文件大小（byte）")
    private Long fileSize;

    @Schema(description = "文件地址")
    private String path;

    @Schema(description = "缩略图文件地址")
    private String thumbnailPath;


}
