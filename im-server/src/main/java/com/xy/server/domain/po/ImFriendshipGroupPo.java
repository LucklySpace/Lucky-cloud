package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName im_friendship_group
 */
@TableName(value = "im_friendship_group")
@Data
public class ImFriendshipGroupPo implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "group_id", type = IdType.AUTO)
    private String groupId;
    /**
     * fromId
     */
    @TableField(value = "from_id")
    private String fromId;
    /**
     *
     */
    @TableField(value = "group_name")
    private String groupName;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;
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
    @TableField(value = "del_flag")
    private Integer delFlag;

}