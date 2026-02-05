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
@Schema(description = "好友分组信息")
@TableName(value = "im_friendship_group")
public class ImFriendshipGroupPo extends BasePo {

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
}
