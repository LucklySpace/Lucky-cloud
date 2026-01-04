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
@Table("audit_log")
@Schema(description = "审计日志")
public class AuditLogPo {

    @Id
    @Column("id")
    private Long id;

    @Column("document_id")
    @Schema(description = "文档ID")
    private Long documentId;

    @Column("action")
    @Schema(description = "操作类型：UPLOAD, DOWNLOAD, DELETE, SEARCH")
    private String action;

    @Column("operator")
    @Schema(description = "操作人")
    private String operator;

    @Column("create_time")
    @Schema(description = "操作时间")
    private LocalDateTime createTime;

    @Column("details")
    @Schema(description = "详情")
    private String details;
}
