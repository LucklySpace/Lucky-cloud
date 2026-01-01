package com.xy.lucky.dubbo.webflux.api.database.group;

import com.xy.lucky.domain.po.ImGroupMemberPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImGroupMemberDubboWebfluxService {

    Flux<ImGroupMemberPo> queryList(String groupId);

    Mono<ImGroupMemberPo> queryOne(String groupId, String memberId);

    Mono<Boolean> removeOne(String memberId);

    Mono<Boolean> creat(ImGroupMemberPo groupMember);

    Mono<Boolean> modify(ImGroupMemberPo groupMember);

    Mono<Boolean> creatBatch(List<ImGroupMemberPo> groupMemberList);

    Flux<String> queryNinePeopleAvatar(String groupId);
}
