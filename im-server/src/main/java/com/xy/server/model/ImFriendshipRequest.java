package com.xy.server.model;

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
public class ImFriendshipRequest implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(value = "id")
    private String id;
    /**
     * from_id
     */
    @TableField(value = "from_id")
    private String from_id;
    /**
     * to_id
     */
    @TableField(value = "to_id")
    private String to_id;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
    /**
     * 是否已读 1已读
     */
    @TableField(value = "read_status")
    private Integer read_status;
    /**
     * 好友来源
     */
    @TableField(value = "add_source")
    private String add_source;
    /**
     * 好友验证信息
     */
    @TableField(value = "add_wording")
    private String add_wording;
    /**
     * 审批状态 1同意 2拒绝
     */
    @TableField(value = "approve_status")
    private Integer approve_status;
    /**
     *
     */
    @TableField(value = "create_time")
    private Long create_time;
    /**
     *
     */
    @TableField(value = "update_time")
    private Long update_time;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;


}