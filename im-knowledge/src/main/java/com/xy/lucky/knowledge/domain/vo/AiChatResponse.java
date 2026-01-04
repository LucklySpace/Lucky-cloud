package com.xy.lucky.knowledge.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI回答响应")
public class AiChatResponse {

    @Schema(description = "回答内容")
    private String answer;

    @Schema(description = "引用文档")
    private List<String> sourceDocuments;
}
