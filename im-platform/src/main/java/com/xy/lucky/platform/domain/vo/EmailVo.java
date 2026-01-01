package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "EmailVo", description = "邮件发送请求")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailVo {

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
