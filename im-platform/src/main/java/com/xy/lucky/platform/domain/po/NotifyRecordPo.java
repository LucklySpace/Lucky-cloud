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
@Schema(description = "通知记录（邮件/短信）")
@Entity
@Table(name = "im_notify_record", indexes = {
        @Index(name = "idx_notify_type_target", columnList = "type,target"),
        @Index(name = "idx_notify_status", columnList = "status")
})
public class NotifyRecordPo {

    @Schema(description = "主键")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Schema(description = "通知类型：EMAIL 或 SMS")
    @Column(name = "type", length = 16, nullable = false)
    private String type;

    @Schema(description = "供应商")
    @Column(name = "provider", length = 32)
    private String provider;

    @Schema(description = "目标（邮箱/手机号）")
    @Column(name = "target", length = 128, nullable = false)
    private String target;

    @Schema(description = "标题（邮件主题/短信模板ID）")
    @Column(name = "title", length = 256)
    private String title;

    @Schema(description = "内容（邮件正文/短信模板参数JSON）")
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Schema(description = "状态：0失败、1成功")
    @Column(name = "status", nullable = false)
    private Integer status;

    @Schema(description = "错误信息")
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Schema(description = "创建时间")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
