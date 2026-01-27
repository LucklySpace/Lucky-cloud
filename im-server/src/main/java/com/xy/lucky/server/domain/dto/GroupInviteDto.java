package com.xy.lucky.server.domain.dto;

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
public class GroupInviteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "群id不能为空")
    @Schema(description = "群聊id")
    private String groupId;

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "被邀请用户id")
    private List<String> memberIds;

    @Schema(description = "邀请类型")
    private Integer type;

    @Schema(description = "邀请信息")
    private String message;

    @Schema(description = "群名称")
    private String groupName;

    @Schema(description = "邀请人id")
    private String inviterId;

    @Schema(description = "邀请请求ID")
    private String requestId;

    @Schema(description = "验证者用户ID（群主或管理员）")
    private String verifierId;

    @Schema(description = "邀请来源")
    private String addSource;

    @Schema(description = "被邀请人状态 （0:待处理, 1:同意, 2:拒绝）")
    private Integer approveStatus;

    @Schema(description = "群主或管理员验证 （0:待处理, 1:同意, 2:拒绝）")
    private Integer verifierStatus;
}
