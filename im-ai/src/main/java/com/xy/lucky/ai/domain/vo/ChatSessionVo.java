package com.xy.lucky.ai.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ChatSessionVo", description = "会话")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSessionVo {

    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "提示ID")
    private String promptId;

    @Schema(description = "会话摘要")
    private String summary;

    @Schema(description = "消息数量")
    private Integer messageCount;

    @Schema(description = "会话状态")
    private String status;

    @Schema(description = "会话分类")
    private String category;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "会话消息")
    private List<ChatMessageVo> messages;
}
