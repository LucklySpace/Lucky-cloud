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
 * @TableName im_friendship_request
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友请求信息")
@TableName(value = "im_friendship_request")
public class ImFriendshipRequestPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

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
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private Long updateTime;

    /**
     * 序列号
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 删除标识（1正常，0删除）
     */
    @TableLogic(value = "1", delval = "0")
    @TableField(value = "del_flag")
    private Integer delFlag;

    @Version
    private Integer version;
}
