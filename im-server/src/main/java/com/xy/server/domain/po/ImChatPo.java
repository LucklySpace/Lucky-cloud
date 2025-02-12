package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @TableName im_chat_set
 */
@TableName(value = "im_chat")
@Data
@Accessors(chain = true)
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

}