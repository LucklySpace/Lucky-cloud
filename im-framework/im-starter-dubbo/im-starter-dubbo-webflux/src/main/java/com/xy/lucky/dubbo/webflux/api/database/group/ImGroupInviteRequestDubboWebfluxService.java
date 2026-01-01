package com.xy.lucky.dubbo.webflux.api.database.group;

import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImGroupInviteRequestDubboWebfluxService {

    Flux<ImGroupInviteRequestPo> queryList(String userId);

    Mono<ImGroupInviteRequestPo> queryOne(ImGroupInviteRequestPo imGroupInviteRequestPo);

    Mono<Boolean> creat(ImGroupInviteRequestPo imGroupInviteRequestPo);

    Mono<Boolean> modify(ImGroupInviteRequestPo imGroupInviteRequestPo);

    Mono<Boolean> removeOne(String requestId);

    Mono<Boolean> creatBatch(List<ImGroupInviteRequestPo> requests);
}
