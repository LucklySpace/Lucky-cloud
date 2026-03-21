package com.xy.lucky.message.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 消息确认请求。
 */
@Data
public class MessageAckDto {

    /**
     * 消息 ID。
     */
    @NotBlank(message = "messageId 不能为空")
    private String messageId;

    /**
     * 确认用户 ID。
     */
    @NotBlank(message = "userId 不能为空")
    private String userId;
}
