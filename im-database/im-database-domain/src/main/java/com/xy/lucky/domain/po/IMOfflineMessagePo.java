package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
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
@Schema(description = "离线消息持久化实体")
@TableName(value = "im_offline_message", excludeProperty = {"createTime", "updateTime", "delFlag", "version"})
public class IMOfflineMessagePo extends BasePo {

    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("message_id")
    private String messageId;

    @TableField("message_type")
    private Integer messageType;

    @TableField("payload")
    private String payload;

    @TableField("created_at")
    private Long createdAt;

    @TableField("expire_at")
    private Long expireAt;
}
