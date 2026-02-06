package com.xy.lucky.rpc.api.oss.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "文件上传信息")
public class OssFileDto {

    @Schema(description = "分片上传的uploadId")
    private String uploadId;

    @Schema(description = "桶名称")
    private String bucketName;

    @Schema(description = "文件唯一标识（md5）")
    @NotBlank
    private String identifier;

    @Schema(description = "文件名")
    @NotBlank
    private String fileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件的key")
    private String objectKey;

    @Schema(description = "文件类型")
    private String contentType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "每个分片大小")
    private Long partSize;

    @Schema(description = "分片数量")
    @NotNull
    @Min(1)
    private Integer partNum;

}
