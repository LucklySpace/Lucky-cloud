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
@Table("document_version")
@Schema(description = "文档版本")
public class DocumentVersionPo {

    @Id
    @Column("id")
    @Schema(description = "ID")
    private Long id;

    @Column("document_id")
    @Schema(description = "文档ID")
    private Long documentId;

    @Column("version_number")
    @Schema(description = "版本号")
    private Integer versionNumber;

    @Column("storage_path")
    @Schema(description = "存储路径")
    private String storagePath;

    @Column("size")
    @Schema(description = "文件大小")
    private Long size;

    @Column("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Column("creator")
    @Schema(description = "创建人")
    private String creator;
}
