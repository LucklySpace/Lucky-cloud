package com.xy.lucky.platform.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 表情包（Emoji Pack）实体
 * - 代表一个可被 IM 应用使用的表情包集合
 * - 支持启用/禁用、封面图、说明等元信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "表情包信息")
@Entity
@Table(name = "im_platform_emoji_pack",
        indexes = {
                @Index(name = "idx_pack_code", columnList = "code"),
                @Index(name = "idx_pack_name", columnList = "name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pack_code", columnNames = {"code"})
        }
)
public class EmojiPackPo {

    @Schema(description = "主键")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Schema(description = "包编码（唯一）", example = "default")
    @Column(name = "code", length = 64, nullable = false, unique = true)
    private String code;

    @Schema(description = "包名称", example = "默认表情包")
    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Schema(description = "包说明")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Schema(description = "封面图桶名")
    @Column(name = "bucket", length = 128)
    private String bucket;

    @Schema(description = "封面图对象Key")
    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Schema(description = "封面图URL（预签名或公网地址）")
    @Column(name = "url", length = 512)
    private String url;

    @Schema(description = "是否启用")
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;

    @Schema(description = "创建时间")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

