package com.xy.server.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群聊邀请")
public class GroupInviteDto  implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "群id不能为空")
    @Schema(description = "群聊id")
    private String group_id;

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户id")
    private String user_id;

    @NotNull(message = "被邀请用户id不能为空")
    @Schema(description = "被邀请用户id")
    private List<String> memberIds;

    @Schema(description = "邀请类型")
    private Integer type;
}
