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
@Schema(description = "用户-表情包关联信息")
@TableName(value = "im_user_emoji_pack")
public class ImUserEmojiPackPo extends BasePo {

    @TableId(value = "id")
    private String id;

    @TableField(value = "user_id")
    private String userId;

    @TableField(value = "pack_id")
    private String packId;
}

