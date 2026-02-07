package com.xy.lucky.chat.domain.dto;

import com.xy.lucky.chat.domain.dto.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 好友请求 DTO
 *
 * @author xy
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "好友请求对象")
public class FriendRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请求ID不能为空", groups = {ValidationGroups.Approve.class})
    @Size(max = 64, message = "请求ID长度不能超过64个字符")
    @Schema(description = "好友请求ID")
    private String id;

    @NotBlank(message = "请求者ID不能为空", groups = {ValidationGroups.Create.class})
    @Size(max = 64, message = "请求者ID长度不能超过64个字符")
    @Schema(description = "请求者ID")
    private String fromId;

    @NotBlank(message = "目标用户ID不能为空", groups = {ValidationGroups.Create.class})
    @Size(max = 64, message = "目标用户ID长度不能超过64个字符")
    @Schema(description = "目标用户ID")
    private String toId;

    @Size(max = 50, message = "备注长度不能超过50个字符")
    @Schema(description = "好友备注")
    private String remark;

    @Size(max = 500, message = "验证消息长度不能超过500个字符")
    @Schema(description = "验证消息")
    private String message;

    @NotNull(message = "审批状态不能为空", groups = {ValidationGroups.Approve.class})
    @Schema(description = "审批状态 (0: 待处理, 1: 同意, 2: 拒绝)")
    private Integer approveStatus;
}
