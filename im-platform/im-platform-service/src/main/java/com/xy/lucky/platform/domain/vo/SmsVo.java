package com.xy.lucky.platform.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "SmsVo", description = "短信发送请求")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsVo {

    @Schema(description = "手机号")
    @NotBlank(message = "phone 不能为空")
    @Pattern(regexp = "^\\+?\\d{6,18}$", message = "phone 格式错误")
    private String phone;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "模板参数")
    private List<String> templateParams;
}
