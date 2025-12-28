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
 * @TableName im_friendship_group
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友分组信息")
@TableName(value = "im_friendship_group")
public class ImFriendshipGroupPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableField(value = "from_id")
    private String fromId;

    /**
     * 分组ID
     */
    @TableId(value = "group_id")
    private String groupId;

    /**
     * 分组名称
     */
    @TableField(value = "group_name")
    private String groupName;

    /**
     * 序列号
     */
    @TableField(value = "sequence")
    private Long sequence;

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
     * 删除标识（1正常，0删除）
     */
    @TableLogic(value = "1", delval = "0")
    @TableField(value = "del_flag")
    private Integer delFlag;

    @Version
    private Integer version;
}
