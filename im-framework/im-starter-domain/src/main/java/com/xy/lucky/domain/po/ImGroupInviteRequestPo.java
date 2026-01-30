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
@Schema(description = "群邀请请求信息")
@TableName(value = "im_group_invite_request")
public class ImGroupInviteRequestPo extends BasePo {

    /**
     * 邀请请求ID
     */
    @TableId(value = "request_id")
    private String requestId;

    /**
     * 群组ID
     */
    @TableField(value = "group_id")
    private String groupId;

    /**
     * 邀请发起者用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 被邀请者用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 验证者用户ID（群主或管理员）
     */
    @TableField(value = "verifier_id")
    private String verifierId;

    /**
     * 群主或管理员验证 （0:待处理, 1:同意, 2:拒绝）
     */
    @TableField(value = "verifier_status")
    private Integer verifierStatus;

    /**
     * 邀请验证信息
     */
    @TableField(value = "message")
    private String message;

    /**
     * 被邀请人状态（0:待处理, 1:同意, 2:拒绝）
     */
    @TableField(value = "approve_status")
    private Integer approveStatus;

    /**
     * 邀请来源（如二维码、成员邀请等）
     */
    @TableField(value = "add_source")
    private Integer addSource;

    /**
     * 邀请过期时间（Unix时间戳）
     */
    @TableField(value = "expire_time")
    private Long expireTime;
}
