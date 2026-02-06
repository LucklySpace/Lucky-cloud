package com.xy.lucky.logging.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "日志")
@Entity
@Table(name = "im_logs", indexes = {
        @Index(name = "idx_ts", columnList = "ts"),
        @Index(name = "idx_module", columnList = "module"),
        @Index(name = "idx_level", columnList = "level"),
        @Index(name = "idx_trace_id", columnList = "trace_id")
})
@Access(AccessType.FIELD)
public class LogPo {

    /**
     * 使用 UUID 类型作为主键，配合 Hibernate 在 Java 端生成 UUID，
     * 在 PostgreSQL 中该列为 uuid 类型（更语义化、索引效率高）。
     */
    @Id
    @Schema(description = "日志id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Schema(description = "时间戳")
    @Column(name = "ts", nullable = false)
    private LocalDateTime ts;

    @Schema(description = "日志级别，如 INFO, ERROR 等，用于过滤和分类日志", example = "ERROR")
    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Schema(description = "模块")
    @Column(name = "module", nullable = false, length = 100)
    private String module;

    @Schema(description = "服务")
    @Column(name = "service", nullable = false, length = 100)
    private String service;

    @Schema(description = "服务实例的地址，如 IP 或主机名，用于定位日志来源", example = "192.168.1.100:8080")
    @Column(name = "address", length = 100)
    private String address;

    @Schema(description = "环境")
    @Column(name = "env", length = 32)
    private String env;

    @Schema(description = "traceId")
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Schema(description = "spanId")
    @Column(name = "span_id", length = 64)
    private String spanId;

    @Schema(description = "线程")
    @Column(name = "thread", length = 100)
    private String thread;

    @Schema(description = "日志内容")
    @Column(name = "message", columnDefinition = "text", nullable = false)
    private String message;

    @Schema(description = "异常")
    @Column(name = "exception", columnDefinition = "text")
    private String exception;

    @Schema(description = "标签（JSON数组文本或逗号分隔文本）")
    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    @Schema(description = "上下文（JSON对象文本）")
    @Column(name = "context", columnDefinition = "text")
    private String context;

    /**
     * 若调用方未设置 ts，则在持久化前填充当前时间点（UTC）。
     */
    @PrePersist
    protected void prePersist() {
        if (this.ts == null) {
            this.ts = LocalDateTime.now();
        }
    }
}
