package com.xy.server.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @TableName im_friendship
 */
@TableName(value = "im_friendship")
@Data
@Accessors(chain = true)
public class ImFriendship implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    /**
     * owner_id
     */
    @TableField(value = "owner_id")
    private String owner_id;
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
     * 状态 1正常 2删除
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 1正常 2拉黑
     */
    @TableField(value = "black")
    private Integer black;
    /**
     *
     */
    @TableField(value = "create_time")
    private Long create_time;
    /**
     *
     */
    @TableField(value = "sequence")
    private Long sequence;
    /**
     *
     */
    @TableField(value = "black_sequence")
    private Long black_sequence;
    /**
     * 来源
     */
    @TableField(value = "add_source")
    private String add_source;
    /**
     * 来源
     */
    @TableField(value = "extra")
    private String extra;


}