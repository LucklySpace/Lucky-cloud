package com.xy.lucky.server.service;


import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import com.xy.lucky.general.response.domain.Result;


public interface GroupService {

    Result getGroupMembers(GroupDto groupDto);

    Result quitGroup(GroupDto groupDto);

    Result inviteGroup(GroupInviteDto groupInviteDto);

    Result groupInfo(GroupDto groupDto);

    Result updateGroupInfo(GroupDto groupDto);

    Result approveGroupInvite(GroupInviteDto groupInviteDto);

    Result updateGroupMember(GroupMemberDto groupMemberDto);
}