package com.xy.lucky.knowledge.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识库文档VO")
public class DocumentVo {

    @Schema(description = "文档ID")
    private Long id;

    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "原始文件名")
    private String originalFilename;

    @Schema(description = "文件类型")
    private String contentType;

    @Schema(description = "文件大小")
    private Long size;

    @Schema(description = "状态：0-上传中 1-处理中 2-已索引 3-失败")
    private Integer status;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "下载链接")
    private String downloadUrl;

    @Schema(description = "分组ID")
    private Long groupId;

    @Schema(description = "分组名称")
    private String groupName;
}
