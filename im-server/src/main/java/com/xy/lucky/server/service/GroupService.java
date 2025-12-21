package com.xy.lucky.server.service;


import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import com.xy.lucky.general.response.domain.Result;

import java.util.Map;


public interface GroupService {

    Map<?, ?> getGroupMembers(GroupDto groupDto);

    void quitGroup(GroupDto groupDto);

    String inviteGroup(GroupInviteDto groupInviteDto);

    Result groupInfo(GroupDto groupDto);

    Result updateGroupInfo(GroupDto groupDto);

    Result approveGroupInvite(GroupInviteDto groupInviteDto);

    Result updateGroupMember(GroupMemberDto groupMemberDto);
}