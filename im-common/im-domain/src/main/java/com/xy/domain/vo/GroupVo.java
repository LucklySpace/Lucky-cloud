package com.xy.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class GroupVo {

    private String groupId;

    private String ownerId;

    private Integer groupType;

    private String groupName;

    private Integer mute;

    private Integer applyJoinType;

    private String avatar;

    private Integer memberCount;

    private String introduction;

    private String notification;

}
