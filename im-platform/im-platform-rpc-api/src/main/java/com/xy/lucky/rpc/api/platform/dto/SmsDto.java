package com.xy.lucky.rpc.api.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 短信发送请求 DTO
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SmsDto", description = "短信发送请求")
public class SmsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "手机号")
    @NotBlank(message = "phone 不能为空")
    @Pattern(regexp = "^\\+?\\d{6,18}$", message = "phone 格式错误")
    private String phone;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "模板参数")
    private List<String> templateParams;
}
