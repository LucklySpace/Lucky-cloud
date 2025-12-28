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
@Table("im_chat")
@Schema(description = "用户会话实体")
public class ImChatEntity {

    @Id
    @Schema(description = "会话ID")
    @Column("chat_id")
    private String chatId;

    @Schema(description = "会话类型")
    @Column("chat_type")
    private Integer chatType;

    @Schema(description = "所有者ID")
    @Column("owner_id")
    private String ownerId;

    @Schema(description = "对端ID")
    @Column("to_id")
    private String toId;

    @Schema(description = "是否静音")
    @Column("is_mute")
    private Integer isMute;

    @Schema(description = "是否置顶")
    @Column("is_top")
    private Integer isTop;

    @Schema(description = "会话序列")
    @Column("sequence")
    private Long sequence;

    @Schema(description = "已读序列")
    @Column("read_sequence")
    private Long readSequence;

    @Schema(description = "创建时间")
    @Column("create_time")
    private Long createTime;

    @Schema(description = "更新时间")
    @Column("update_time")
    private Long updateTime;

    @Schema(description = "删除标志")
    @Column("del_flag")
    private Integer delFlag;

    @Schema(description = "版本号")
    @Column("version")
    private Integer version;
}
