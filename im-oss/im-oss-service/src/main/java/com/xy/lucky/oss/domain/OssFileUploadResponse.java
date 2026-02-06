package com.xy.lucky.oss.domain;

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
@Schema(description = "文件上传响应对象")
public class OssFileUploadResponse {

    @Schema(description = "对象存储地址")
    private String s3Url;

    @Schema(description = "公开访问地址")
    private String publicUrl;

}
