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
@Schema(description = "群聊消息阅读状态")
@TableName(value = "im_group_message_status", excludeProperty = {"delFlag"})
public class ImGroupMessageStatusPo extends BasePo {

    /**
     * 群组ID
     */
    @TableId(value = "group_id")
    private String groupId;

    /**
     * 消息ID
     */
    @TableField(value = "message_id")
    private String messageId;

    /**
     * 接收者用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 阅读状态（1已读）
     */
    @TableField(value = "read_status")
    private Integer readStatus;
}
