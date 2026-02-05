package com.xy.lucky.database.webflux.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("im_outbox")
@Schema(description = "消息出箱实体")
public class ImOutboxEntity {

    @Id
    @Schema(description = "出箱记录ID")
    @Column("id")
    private Long id;

    @Schema(description = "消息ID")
    @Column("message_id")
    private String messageId;

    @Schema(description = "消息载荷")
    @Column("payload")
    private String payload;

    @Schema(description = "交换机")
    @Column("exchange")
    private String exchange;

    @Schema(description = "路由键")
    @Column("routing_key")
    private String routingKey;

    @Schema(description = "尝试次数")
    @Column("attempts")
    private Integer attempts;

    @Schema(description = "状态")
    @Column("status")
    private String status;

    @Schema(description = "最后错误")
    @Column("last_error")
    private String lastError;

    @Schema(description = "下次重试时间")
    @Column("next_try_at")
    private Long nextTryAt;

    @Schema(description = "创建时间")
    @Column("created_at")
    private Long createdAt;

    @Schema(description = "更新时间")
    @Column("updated_at")
    private Long updatedAt;
}
