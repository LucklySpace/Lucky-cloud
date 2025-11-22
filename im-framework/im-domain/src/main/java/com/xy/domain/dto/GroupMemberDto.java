package com.xy.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群成员")
public class GroupMemberDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "群id不能为空")
    @Schema(description = "群id")
    private String groupId;

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "群内昵称")
    private String alias;

    @Schema(description = "群备注")
    private String remark;
}
