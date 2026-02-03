package com.xy.lucky.oss.domain.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "分片上传信息")
public class FileChunkVo {

    /**
     * 上传地址
     */
    @Schema(description = "上传地址")
    private Map<String, String> uploadUrl;

    /**
     * 上传id
     */
    @Schema(description = "上传id")
    private String uploadId;
}
