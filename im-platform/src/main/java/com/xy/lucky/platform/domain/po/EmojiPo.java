package com.xy.lucky.platform.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 单个表情条目
 * - 归属于一个表情包
 * - 存储于 MinIO（记录桶/对象键/类型/大小等）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "表情条目")
@Entity
@Table(name = "im_platform_emoji",
        indexes = {
                @Index(name = "idx_emoji_pack", columnList = "pack_id"),
                @Index(name = "idx_emoji_name", columnList = "name")
        }
)
public class EmojiPo {

    @Schema(description = "主键")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    private EmojiPackPo pack;

    @Schema(description = "表情名称", example = "smile")
    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Schema(description = "标签（逗号分隔）", example = "funny,happy")
    @Column(name = "tags", length = 256)
    private String tags;

    @Schema(description = "桶名称")
    @Column(name = "bucket", length = 128, nullable = false)
    private String bucket;

    @Schema(description = "对象Key")
    @Column(name = "object_key", length = 512, nullable = false)
    private String objectKey;

    @Schema(description = "外网访问URL（预签名）")
    @Column(name = "url", length = 512)
    private String url;

    @Schema(description = "顺序")
    @Column(name = "sort")
    private Integer sort;

    @Schema(description = "内容类型")
    @Column(name = "content_type", length = 128)
    private String contentType;

    @Schema(description = "文件大小")
    @Column(name = "file_size")
    private Long fileSize;

    @Schema(description = "创建时间")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}

