package com.xy.lucky.rpc.api.oss.dto;

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
@Schema(description = "文件下载范围")
public class FileDownloadRangeDto {

    @Schema(description = "开始位置")
    private Long start;

    @Schema(description = "结束位置")
    private Long end;

    @Schema(description = "文件大小")
    private Long fileSize;

}
