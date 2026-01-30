package com.xy.lucky.file.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "文件")
public class FileVo {

    @Schema(description = "文件md5")
    private String identifier;

    @Schema(description = "文件名称")
    private String name;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "文件类型")
    private String type;

    @Schema(description = "文件地址")
    private String path;

    @Schema(description = "缩略图文件地址")
    private String thumbnailPath;

}
