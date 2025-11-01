package com.xy.server.service;


import com.xy.domain.dto.GroupDto;
import com.xy.domain.dto.GroupInviteDto;
import com.xy.domain.dto.GroupMemberDto;
import com.xy.general.response.domain.Result;


public interface GroupService {

    Result getGroupMembers(GroupDto groupDto);

    Result quitGroup(GroupDto groupDto);

    Result inviteGroup(GroupInviteDto groupInviteDto);

    Result groupInfo(GroupDto groupDto);

    Result updateGroupInfo(GroupDto groupDto);

    Result approveGroupInvite(GroupInviteDto groupInviteDto);

    Result updateGroupMember(GroupMemberDto groupMemberDto);
}