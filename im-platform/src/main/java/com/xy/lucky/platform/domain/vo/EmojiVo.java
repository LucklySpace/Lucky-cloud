package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 表情条目请求/响应对象
 */
@Data
@Schema(name = "EmojiVo", description = "表情条目：包含名称、所属包、对象存储信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmojiVo {

    @Schema(description = "所属表情包ID")
    @NotBlank(message = "packId 不能为空")
    @Size(max = 64, message = "packId 最长 64 字符")
    private String packId;

    @Schema(description = "表情名称", example = "smile")
    @NotBlank(message = "name 不能为空")
    @Size(max = 128, message = "name 最长 128 字符")
    private String name;

    @Schema(description = "标签（逗号分隔）", example = "happy,funny")
    @Size(max = 256, message = "tags 最长 256 字符")
    private String tags;

    @Schema(description = "表情ID")
    private String emojiId;

    @Schema(description = "下载URL（预签名）")
    private String url;
}

