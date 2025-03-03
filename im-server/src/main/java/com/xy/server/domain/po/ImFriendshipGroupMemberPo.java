package com.xy.server.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * @TableName im_friendship_group_member
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "im_friendship_group_member")
public class ImFriendshipGroupMemberPo implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * 分组ID
     */
    @TableId(value = "group_id")
    private String groupId;
    /**
     * 好友用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Long createTime;

    /**
     * 删除标识（0正常，1删除）
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(value = "del_flag")
    private Integer delFlag;

    @Version
    private Integer version;

}