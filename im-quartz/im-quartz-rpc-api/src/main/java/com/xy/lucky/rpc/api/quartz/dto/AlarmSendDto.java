package com.xy.lucky.rpc.api.quartz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 告警发送 DTO
 *
 * @author Lucky
 */
@Data
@Schema(description = "告警发送DTO")
public class AlarmSendDto implements Serializable {

    @Schema(description = "接收人邮箱")
    @NotBlank(message = "接收人邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "邮件主题")
    @NotBlank(message = "邮件主题不能为空")
    private String subject;

    @Schema(description = "邮件内容")
    @NotBlank(message = "邮件内容不能为空")
    private String content;

    @Schema(description = "是否HTML格式")
    private Boolean isHtml = false;
}
