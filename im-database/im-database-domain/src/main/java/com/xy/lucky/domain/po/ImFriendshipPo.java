package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.lucky.domain.BasePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友关系信息")
@TableName(value = "im_friendship")
public class ImFriendshipPo extends BasePo {

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
}
