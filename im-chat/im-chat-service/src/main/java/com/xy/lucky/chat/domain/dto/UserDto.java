package com.xy.lucky.chat.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.lucky.chat.domain.dto.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;

/**
 * 用户 DTO
 *
 * @author xy
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户对象")
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户ID不能为空", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class, ValidationGroups.Query.class})
    @Size(max = 64, message = "用户ID长度不能超过64个字符")
    @Schema(description = "用户ID")
    private String userId;

    @Size(max = 50, message = "用户名称长度不能超过50个字符")
    @Schema(description = "用户名称")
    private String name;

    @Size(max = 500, message = "用户头像URL长度不能超过500个字符")
    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "用户性别 (0: 未知, 1: 男, 2: 女)")
    private Integer gender;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "用户生日")
    private String birthday;

    @Size(max = 100, message = "用户地址长度不能超过100个字符")
    @Schema(description = "用户地址")
    private String location;

    @Size(max = 200, message = "用户签名长度不能超过200个字符")
    @Schema(description = "用户签名")
    private String selfSignature;
}
