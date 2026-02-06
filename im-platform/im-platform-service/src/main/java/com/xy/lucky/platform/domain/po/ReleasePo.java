package com.xy.lucky.platform.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "应用更新发布记录")
@Entity
@Table(name = "im_platform_release")
public class ReleasePo {

    @Schema(description = "主键")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Schema(description = "应用ID")
    @Column(name = "app_id", length = 64, nullable = false)
    private String appId;

    @Schema(description = "版本号")
    @Column(name = "version", length = 64, unique = true, nullable = false)
    private String version;

    @Schema(description = "更新说明")
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Schema(description = "发布时间")
    @Column(name = "pub_date", length = 64)
    private String pubDate;

    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetPo> assets;

    @Schema(description = "创建时间")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

}
