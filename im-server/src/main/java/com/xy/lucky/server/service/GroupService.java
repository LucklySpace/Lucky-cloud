package com.xy.lucky.server.service;


import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import reactor.core.publisher.Mono;

import java.util.Map;


public interface GroupService {

    Mono<Map<?, ?>> getGroupMembers(GroupDto groupDto);

    Mono<Void> quitGroup(GroupDto groupDto);

    Mono<String> inviteGroup(GroupInviteDto groupInviteDto);

    Mono<com.xy.lucky.domain.po.ImGroupPo> groupInfo(GroupDto groupDto);

    Mono<Boolean> updateGroupInfo(GroupDto groupDto);

    Mono<String> approveGroupInvite(GroupInviteDto groupInviteDto);

    Mono<Boolean> updateGroupMember(GroupMemberDto groupMemberDto);
}
