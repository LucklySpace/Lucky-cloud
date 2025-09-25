package com.xy.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * 群聊邀请请求
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "im_group_invite_request")
public class ImGroupInviteRequestPo implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private String addSource;

    /**
     * 邀请过期时间（Unix时间戳）
     */
    @TableField(value = "expire_time")
    private Long expireTime;

    /**
     * 创建时间（Unix时间戳）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间（Unix时间戳）
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private Long updateTime;

    /**
     * 删除标识（1:正常, 0:删除）
     */
    @TableLogic(value = "1", delval = "0")
    @TableField(value = "del_flag")
    private Integer delFlag;

    /**
     * 版本信息（用于乐观锁）
     */
    @Version
    private Long version;
}