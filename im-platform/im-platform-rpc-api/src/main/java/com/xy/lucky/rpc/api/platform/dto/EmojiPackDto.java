package com.xy.lucky.rpc.api.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表情包创建请求 DTO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmojiPackDto", description = "表情包创建请求")
public class EmojiPackDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "包编码（唯一）")
    private String code;

    @Schema(description = "包名称", example = "默认表情包")
    @NotBlank(message = "name 不能为空")
    @Size(max = 128, message = "name 最长 128 字符")
    private String name;

    @Schema(description = "包说明")
    @Size(max = 5000, message = "说明最长 5000 字符")
    private String description;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
