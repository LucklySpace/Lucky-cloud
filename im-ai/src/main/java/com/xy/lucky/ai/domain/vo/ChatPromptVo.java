package com.xy.lucky.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ChatPromptVo", description = "提示词")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatPromptVo {

    @Schema(description = "提示词ID")
    private String id;

    @Schema(description = "提示词名称")
    private String name;

    @Schema(description = "提示词内容")
    private String prompt;

    @Schema(description = "提示词描述")
    private String description;

}
