package com.xy.lucky.server.domain.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户对象")
public class UserDto {

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "用户名称")
    private String name;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "用户性别")
    private Integer gender;

    @Schema(description = "用户生日")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String birthday;

    @Schema(description = "用户地址")
    private String location;

    @Schema(description = "用户签名")
    private String selfSignature;
}
