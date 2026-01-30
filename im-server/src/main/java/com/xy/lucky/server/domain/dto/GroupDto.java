package com.xy.lucky.server.domain.dto;

import com.xy.lucky.server.domain.dto.validation.ValidationGroups;
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
 * 群组 DTO
 *
 * @author xy
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "群聊对象")
public class GroupDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "群组ID不能为空", groups = {
            ValidationGroups.Query.class, ValidationGroups.Update.class,
            ValidationGroups.Delete.class, ValidationGroups.SetJoinMode.class,
            ValidationGroups.MuteAll.class, ValidationGroups.DismissGroup.class
    })
    @Size(max = 64, message = "群组ID长度不能超过64个字符")
    @Schema(description = "群聊ID")
    private String groupId;

    @NotBlank(message = "用户ID不能为空", groups = {
            ValidationGroups.Delete.class, ValidationGroups.SetJoinMode.class,
            ValidationGroups.MuteAll.class, ValidationGroups.DismissGroup.class
    })
    @Size(max = 64, message = "用户ID长度不能超过64个字符")
    @Schema(description = "操作者用户ID")
    private String userId;

    @Size(max = 50, message = "群名称长度不能超过50个字符")
    @Schema(description = "群名称")
    private String groupName;

    @Size(max = 500, message = "群头像URL长度不能超过500个字符")
    @Schema(description = "群头像")
    private String avatar;

    @Size(max = 500, message = "群简介长度不能超过500个字符")
    @Schema(description = "群简介")
    private String introduction;

    @Size(max = 500, message = "群公告长度不能超过500个字符")
    @Schema(description = "群公告")
    private String notification;

    @NotNull(message = "加入方式不能为空", groups = {ValidationGroups.SetJoinMode.class})
    @Schema(description = "加入方式: 0-禁止申请, 1-需要审批, 2-自由加入")
    private Integer applyJoinType;

    @NotNull(message = "全员禁言状态不能为空", groups = {ValidationGroups.MuteAll.class})
    @Schema(description = "全员禁言: 0-禁言, 1-正常")
    private Integer muteAll;

    @Schema(description = "群类型: 0-公开群, 1-私有群")
    private Integer groupType;

    @Schema(description = "群状态: 0-解散, 1-正常")
    private Integer status;
}
