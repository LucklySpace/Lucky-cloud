package com.xy.lucky.rpc.api.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件发送请求 DTO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "EmailDto", description = "邮件发送请求")
public class EmailDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收件人邮箱")
    @NotBlank(message = "to 不能为空")
    @Email(message = "to 格式错误")
    private String to;

    @Schema(description = "主题")
    @NotBlank(message = "subject 不能为空")
    @Size(max = 256, message = "subject 最长 256 字符")
    private String subject;

    @Schema(description = "内容")
    @NotBlank(message = "content 不能为空")
    private String content;

    @Schema(description = "是否HTML内容")
    private Boolean html = true;
}
