package com.xy.lucky.file.domain.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "文件上传进度")
public class FileUploadProgressVo {

    @Schema(description = "是否为新文件 （从未上传过的文件）,1是 0否")
    private Integer isNew;

    @Schema(description = "是否已完成上传 （是否已经合并分片）,1是 0否")
    private Integer isFinish;

    @Schema(description = "文件地址")
    private String path;

    @Schema(description = "上传id")
    private String uploadId;

    @Schema(description = "未完全上传时,还未上传的(分片、上传链接)Map")
    private Map<String, String> undoneChunkMap;

}
