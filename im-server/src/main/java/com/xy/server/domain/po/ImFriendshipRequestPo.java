package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @TableName im_friendship_request
 */
@TableName(value = "im_friendship_request")
@Data
@Accessors(chain = true)
public class ImFriendshipRequestPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(value = "id")
    private String id;
    /**
     * fromId
     */
    @TableField(value = "from_id")
    private String fromId;
    /**
     * toId
     */
    @TableField(value = "to_id")
    private String toId;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
    /**
     * 是否已读 1已读
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
     * 审批状态 1同意 2拒绝
     */
    @TableField(value = "approve_status")
    private Integer approveStatus;
    /**
     *
     */
    @TableField(value = "create_time")
    private Long createTime;
    /**
     *
     */
    @TableField(value = "update_time")
    private Long updateTime;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;


}