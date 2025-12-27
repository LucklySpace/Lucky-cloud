package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @TableName im_chat_set
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户聊天会话信息")
@TableName(value = "im_chat")
public class ImChatPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "chat_id")
    private String chatId;
    /**
     * 0 单聊 1群聊 2机器人 3公众号
     */
    @TableField(value = "chat_type")
    private Integer chatType;
    /**
     *
     */
    @TableField(value = "owner_id")
    private String ownerId;
    /**
     *
     */
    @TableField(value = "to_id")
    private String toId;
    /**
     * 是否免打扰 1免打扰
     */
    @TableField(value = "is_mute")
    private Integer isMute;
    /**
     * 是否置顶 1置顶
     */
    @TableField(value = "is_top")
    private Integer isTop;
    /**
     * sequence
     */
    @TableField(value = "sequence")
    private Long sequence;
    /**
     * 已读序列号
     */
    @TableField(value = "read_sequence")
    private Long readSequence;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 修改时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private Long updateTime;

    /**
     * 删除标识（1正常，0删除）
     */
    @TableLogic(value = "1", delval = "0")
    @TableField(value = "del_flag")
    private Integer delFlag;

    @Version
    private Integer version;

}
