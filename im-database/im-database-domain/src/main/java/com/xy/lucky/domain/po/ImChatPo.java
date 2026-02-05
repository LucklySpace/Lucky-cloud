package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.lucky.domain.BasePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户聊天会话信息")
@TableName(value = "im_chat")
public class ImChatPo extends BasePo {

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
