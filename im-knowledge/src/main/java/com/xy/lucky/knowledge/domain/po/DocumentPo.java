package com.xy.lucky.knowledge.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_document")
@Schema(description = "知识库文档")
public class DocumentPo {

    @Id
    @Column("id")
    @Schema(description = "文档ID")
    private Long id;

    @Column("title")
    @Schema(description = "文档标题")
    private String title;

    @Column("original_filename")
    @Schema(description = "原始文件名")
    private String originalFilename;

    @Column("storage_path")
    @Schema(description = "存储路径")
    private String storagePath;

    @Column("content_type")
    @Schema(description = "文件类型")
    private String contentType;

    @Column("size")
    @Schema(description = "文件大小")
    private Long size;

    @Column("status")
    @Schema(description = "状态：0-上传中 1-处理中 2-已索引 3-失败")
    private Integer status;

    @Column("version")
    @Schema(description = "当前版本号")
    private Integer version;

    @Column("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Column("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Column("creator")
    @Schema(description = "创建人")
    private String creator;

    @Column("permission")
    @Schema(description = "权限设置")
    private String permission;

    @Column("group_id")
    @Schema(description = "分组ID")
    private Long groupId;
}
