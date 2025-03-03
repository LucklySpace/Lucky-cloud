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
 * @TableName im_friendship
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "im_friendship")
public class ImFriendshipPo implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "owner_id")
    private String ownerId;

    /**
     * 好友用户ID
     */
    @TableField(value = "to_id")
    private String toId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 状态（1正常，2删除）
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 黑名单状态（1正常，2拉黑）
     */
    @TableField(value = "black")
    private Integer black;

    /**
     * 序列号
     */
    @TableField(value = "sequence")
    private Long sequence;

    /**
     * 黑名单序列号
     */
    @TableField(value = "black_sequence")
    private Long blackSequence;

    /**
     * 好友来源
     */
    @TableField(value = "add_source")
    private String addSource;

    /**
     * 扩展字段
     */
    @TableField(value = "extra")
    private String extra;

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

    @Version
    private Integer version;
}