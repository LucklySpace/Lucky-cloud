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

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ChatMessageVo", description = "消息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageVo {

    @Schema(name = "id", description = "消息ID")
    private String id;

    @Schema(name = "type", description = "消息类型")
    private String type;

    @Schema(name = "content", description = "消息内容")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(name = "createdAt", description = "创建时间")
    private LocalDateTime createdAt;
}
