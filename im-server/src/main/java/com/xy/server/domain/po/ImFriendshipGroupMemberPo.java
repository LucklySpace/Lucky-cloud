package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * @TableName im_friendship_group_member
 */
@TableName(value = "im_friendship_group_member")
@Data
public class ImFriendshipGroupMemberPo implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     *
     */
    @TableId(value = "group_id")
    private String groupId;
    /**
     *
     */
    @TableField(value = "to_id")
    private String toId;
    
}