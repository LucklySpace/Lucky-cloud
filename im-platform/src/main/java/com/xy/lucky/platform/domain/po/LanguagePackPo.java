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
 * 语言包实体
 * - 唯一标识为 locale（如 zh-CN、en-US）
 * - 文件存储于 MinIO（记录桶名与对象键）
 * - 支持版本、作者、描述等元信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "语言包信息")
@Entity
@Table(name = "im_platform_language_pack",
        indexes = {
                @Index(name = "idx_lang_locale", columnList = "locale"),
                @Index(name = "idx_lang_name", columnList = "name"),
                @Index(name = "idx_lang_version", columnList = "version")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lang_locale", columnNames = {"locale"})
        }
)
public class LanguagePackPo {

    @Schema(description = "主键")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Schema(description = "地区/语言标识", example = "zh-CN")
    @Column(name = "locale", length = 64, nullable = false, unique = true)
    private String locale;

    @Schema(description = "语言包名称", example = "简体中文")
    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Schema(description = "版本号", example = "1.0.0")
    @Column(name = "version", length = 64, nullable = false)
    private String version;

    @Schema(description = "作者")
    @Column(name = "author", length = 128)
    private String author;

    @Schema(description = "描述")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Schema(description = "桶名称")
    @Column(name = "bucket_name", length = 128)
    private String bucketName;

    @Schema(description = "对象Key")
    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Schema(description = "下载地址（预签名或公网）")
    @Column(name = "download_url", length = 512)
    private String downloadUrl;

    @Schema(description = "内容类型")
    @Column(name = "content_type", length = 128)
    private String contentType;

    @Schema(description = "文件大小")
    @Column(name = "size")
    private Long size;

    @Schema(description = "创建时间")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

