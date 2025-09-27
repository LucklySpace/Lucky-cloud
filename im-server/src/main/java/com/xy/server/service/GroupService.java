package com.xy.server.service;


import com.xy.domain.dto.GroupDto;
import com.xy.domain.dto.GroupInviteDto;
import com.xy.general.response.domain.Result;

/**
 * 单聊
 */

public interface GroupService {

    Result getGroupMembers(GroupDto groupDto);

    Result quitGroup(GroupDto groupDto);

    Result inviteGroup(GroupInviteDto groupInviteDto);

    Result groupInfo(GroupDto groupDto);

    Result approveGroupInvite(GroupInviteDto groupInviteDto);

}