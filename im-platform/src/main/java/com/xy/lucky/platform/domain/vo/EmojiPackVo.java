package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 表情包请求/响应对象
 */
@Data
@Schema(name = "EmojiPackVo", description = "表情包元信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmojiPackVo {

    @Schema(description = "包编码（唯一）")
    private String code;

    @Schema(description = "包名称", example = "默认表情包")
    @NotBlank(message = "name 不能为空")
    @Size(max = 128, message = "name 最长 128 字符")
    private String name;

    @Schema(description = "包说明")
    @Size(max = 5000, message = "说明最长 5000 字符")
    private String description;

    @Schema(description = "包ID")
    private String packId;

    @Schema(description = "封面图URL")
    private String url;

    @Schema(description = "是否启用")
    private Boolean enabled;
}

