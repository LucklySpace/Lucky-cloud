package com.xy.lucky.rpc.api.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 表情包信息 VO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmojiPackVo", description = "表情包元信息")
public class EmojiPackVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "包编码（唯一）")
    private String code;

    @Schema(description = "包名称", example = "默认表情包")
    private String name;

    @Schema(description = "包说明")
    private String description;

    @Schema(description = "包ID")
    private String packId;

    @Schema(description = "封面图URL")
    private String url;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
