package com.xy.lucky.message.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 离线消息补发请求。
 */
@Data
public class MessageReplayDto {

    /**
     * 待补发离线消息的用户 ID。
     */
    @NotBlank(message = "userId 不能为空")
    private String userId;
}
