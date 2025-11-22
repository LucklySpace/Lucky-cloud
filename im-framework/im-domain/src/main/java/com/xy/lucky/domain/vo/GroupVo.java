package com.xy.lucky.domain.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
