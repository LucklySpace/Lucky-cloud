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
@Schema(description = "群聊对象")
public class GroupDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "群id不能为空")
    @Schema(description = "群聊id")
    private String groupId;

    @Schema(description = "用户id")
    private String userId;

    @Schema(description = "群名称")
    private String groupName;

    @Schema(description = "群头像")
    private String avatar;

    @Schema(description = "群简介")
    private String introduction;

    @Schema(description = "群公告")
    private String notification;

}
