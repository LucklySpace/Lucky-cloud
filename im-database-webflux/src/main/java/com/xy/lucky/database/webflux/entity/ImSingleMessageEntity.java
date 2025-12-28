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
@Table("im_single_message")
@Schema(description = "单聊消息实体")
public class ImSingleMessageEntity {

    @Id
    @Schema(description = "消息ID")
    @Column("message_id")
    private String messageId;

    @Schema(description = "发送者ID")
    @Column("from_id")
    private String fromId;

    @Schema(description = "接收者ID")
    @Column("to_id")
    private String toId;

    @Schema(description = "消息体")
    @Column("message_body")
    private String messageBody;

    @Schema(description = "消息时间")
    @Column("message_time")
    private Long messageTime;

    @Schema(description = "消息类型")
    @Column("message_content_type")
    private Integer messageContentType;

    @Schema(description = "阅读状态")
    @Column("read_status")
    private Integer readStatus;

    @Schema(description = "扩展字段")
    @Column("extra")
    private String extra;

    @Schema(description = "回复消息")
    @Column("reply_message")
    private String replyMessage;

    @Schema(description = "删除标志")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "序列")
    @Column("sequence")
    private Long sequence;

    @Schema(description = "消息随机码")
    @Column("message_random")
    private String messageRandom;

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
