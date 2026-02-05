package com.xy.lucky.database.webflux.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("im_group_message_status")
@Schema(description = "群消息状态实体")
public class ImGroupMessageStatusEntity {

    @Schema(description = "群ID")
    @Column("group_id")
    private String groupId;

    @Schema(description = "消息ID")
    @Column("message_id")
    private String messageId;

    @Schema(description = "接收者ID")
    @Column("to_id")
    private String toId;

    @Schema(description = "阅读状态")
    @Column("read_status")
    private Integer readStatus;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "版本号")
    @Column("version")
    private Integer version;
}
