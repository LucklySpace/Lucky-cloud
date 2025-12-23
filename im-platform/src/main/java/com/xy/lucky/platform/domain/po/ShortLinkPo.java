package com.xy.lucky.platform.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 短链实体（PostgreSQL 友好版本）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "短链信息")
@Entity
@Table(name = "im_platform_short_link",
        indexes = {
                @Index(name = "idx_short_code", columnList = "short_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_short_code", columnNames = {"short_code"})
        }
)
public class ShortLinkPo {

    @Schema(description = "主键 UUID")
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Schema(description = "短码")
    @Column(name = "short_code", length = 64, nullable = false, unique = true)
    private String shortCode;

    @Schema(description = "原始URL")
    // 使用 text 更灵活，不必强制长度
    @Column(name = "original_url", columnDefinition = "text", nullable = false)
    private String originalUrl;

    @Schema(description = "完整短链", example = "https://s.example.com/api/v1/short/r/a1B2c3")
    @Column(name = "short_url", columnDefinition = "text", nullable = false)
    private String shortUrl;

    @Schema(description = "访问次数")
    @Column(name = "visit_count", nullable = false)
    @Builder.Default
    private Integer visitCount = 0;

    @Schema(description = "过期时间")
    @Column(name = "expire_time", columnDefinition = "timestamp with time zone")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @UpdateTimestamp
    @Column(name = "update_time", columnDefinition = "timestamp with time zone")
    private LocalDateTime updateTime;

    @Schema(description = "是否启用")
    @Column(name = "enabled", nullable = false, columnDefinition = "boolean default true")
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;

    // 辅助：返回 String id（如果你需要 String）
    public String getIdAsString() {
        return id != null ? id.toString() : null;
    }
}
