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
@Schema(description = "好友分组成员信息")
@TableName(value = "im_friendship_group_member", excludeProperty = {"updateTime"})
public class ImFriendshipGroupMemberPo extends BasePo {

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

}
