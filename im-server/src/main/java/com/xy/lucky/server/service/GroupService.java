package com.xy.lucky.server.service;


import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import com.xy.lucky.domain.po.ImGroupPo;

import java.util.Map;

public interface GroupService {

    Map<?, ?> getGroupMembers(GroupDto groupDto);

    void quitGroup(GroupDto groupDto);

    String inviteGroup(GroupInviteDto groupInviteDto);

    ImGroupPo groupInfo(GroupDto groupDto);

    Boolean updateGroupInfo(GroupDto groupDto);

    String approveGroupInvite(GroupInviteDto groupInviteDto);

    Boolean updateGroupMember(GroupMemberDto groupMemberDto);
}
