package com.xy.lucky.rpc.api.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表情条目 VO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmojiVo", description = "表情条目：包含名称、所属包、对象存储信息")
public class EmojiVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "所属表情包ID")
    private String packId;

    @Schema(description = "表情名称", example = "smile")
    private String name;

    @Schema(description = "标签（逗号分隔）", example = "happy,funny")
    private String tags;

    @Schema(description = "表情ID")
    private String emojiId;

    @Schema(description = "下载URL（预签名）")
    private String url;

    @Schema(description = "顺序")
    private Integer sort;
}
