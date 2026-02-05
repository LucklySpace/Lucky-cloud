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
@Schema(description = "好友请求信息")
@TableName(value = "im_friendship_request")
public class ImFriendshipRequestPo extends BasePo {

    /**
     * 请求ID
     */
    @TableId(value = "id")
    private String id;

    /**
     * 请求发起者
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 请求接收者
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 是否已读（1已读）
     */
    @TableField(value = "read_status")
    private Integer readStatus;

    /**
     * 好友来源
     */
    @TableField(value = "add_source")
    private String addSource;

    /**
     * 好友验证信息
     */
    @TableField(value = "message")
    private String message;

    /**
     * 审批状态（0未审批，1同意，2拒绝）
     */
    @TableField(value = "approve_status")
    private Integer approveStatus;

    /**
     * 序列号
     */
    @TableField(value = "sequence")
    private Long sequence;
}
