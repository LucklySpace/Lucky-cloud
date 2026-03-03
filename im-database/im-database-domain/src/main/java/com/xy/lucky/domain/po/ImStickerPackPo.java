package com.xy.lucky.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.lucky.domain.BasePo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "表情包信息")
@TableName(value = "im_sticker_pack")
public class ImStickerPackPo extends BasePo {

    @TableId(value = "id")
    private String id;

    @TableField(value = "code")
    private String code;

    @TableField(value = "name")
    private String name;

    @TableField(value = "description")
    private String description;

    @TableField(value = "bucket")
    private String bucket;

    @TableField(value = "object_key")
    private String objectKey;

    @TableField(value = "url")
    private String url;

    @TableField(value = "enabled")
    @Builder.Default
    private Boolean enabled = Boolean.TRUE;

    @TableField(value = "heat")
    private Long heat;
}
