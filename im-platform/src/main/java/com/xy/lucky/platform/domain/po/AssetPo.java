package com.xy.lucky.platform.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "应用更新平台文件")
@Entity
@Table(name = "im_platform_asset", indexes = {
        @Index(name = "idx_asset_file", columnList = "file_name"),
        @Index(name = "idx_asset_platform", columnList = "platform")
})
public class AssetPo {

    @Schema(description = "主键")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id", nullable = false)
    private ReleasePo release;

    @Schema(description = "平台标识")
    @Column(name = "platform", length = 64, nullable = false)
    private String platform;

    @Schema(description = "MD5")
    @Column(name = "md5", length = 64)
    private String md5;

    @Schema(description = "文件名")
    @Column(name = "file_name", length = 256, nullable = false)
    private String fileName;

    @Schema(description = "下载地址")
    @Column(name = "url", length = 512)
    private String url;

    @Schema(description = "签名")
    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    @Schema(description = "桶名称")
    @Column(name = "bucket_name", length = 128)
    private String bucketName;

    @Schema(description = "对象Key")
    @Column(name = "object_key", length = 512)
    private String objectKey;

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
