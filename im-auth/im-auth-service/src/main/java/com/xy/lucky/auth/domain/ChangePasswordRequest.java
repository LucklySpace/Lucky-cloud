package com.xy.lucky.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @NotBlank(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private String userId;

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码密文（RSA加密）")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码密文（RSA加密）")
    private String newPassword;
}
