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
@Schema(description = "表情条目")
@TableName(value = "im_sticker")
public class ImStickerPo extends BasePo {

    @TableId(value = "id")
    private String id;

    @TableField(value = "pack_id")
    private String packId;

    @TableField(value = "name")
    private String name;

    @TableField(value = "tags")
    private String tags;

    @TableField(value = "bucket")
    private String bucket;

    @TableField(value = "object_key")
    private String objectKey;

    @TableField(value = "url")
    private String url;

    @TableField(value = "sort")
    private Integer sort;

    @TableField(value = "content_type")
    private String contentType;

    @TableField(value = "file_size")
    private Long fileSize;
}
