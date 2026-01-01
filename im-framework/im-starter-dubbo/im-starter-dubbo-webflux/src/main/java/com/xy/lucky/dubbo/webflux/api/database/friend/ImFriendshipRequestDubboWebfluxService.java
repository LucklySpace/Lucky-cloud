package com.xy.lucky.dubbo.webflux.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImFriendshipRequestDubboWebfluxService {

    Flux<ImFriendshipRequestPo> queryList(String userId);

    Mono<ImFriendshipRequestPo> queryOne(ImFriendshipRequestPo request);

    Mono<Boolean> creat(ImFriendshipRequestPo request);

    Mono<Boolean> modify(ImFriendshipRequestPo request);

    Mono<Boolean> removeOne(String requestId);

    Mono<Boolean> modifyStatus(String requestId, Integer status);
}
