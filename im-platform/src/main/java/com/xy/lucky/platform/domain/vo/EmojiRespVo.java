package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "EmojiRespVo", description = "表情包")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmojiRespVo {

    @Schema(description = "包名称", example = "默认表情包")
    private String name;

    @Schema(description = "包说明")
    private String description;

    @Schema(description = "包ID")
    private String packId;

    @Schema(description = "封面图URL")
    private String url;

    @Schema(description = "表情列表")
    private List<EmojiVo> emojis;
}
