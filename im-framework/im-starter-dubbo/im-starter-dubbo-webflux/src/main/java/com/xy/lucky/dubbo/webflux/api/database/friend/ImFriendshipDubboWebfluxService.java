package com.xy.lucky.dubbo.webflux.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImFriendshipDubboWebfluxService {

    Flux<ImFriendshipPo> queryList(String ownerId, Long sequence);

    Mono<ImFriendshipPo> queryOne(String ownerId, String toId);

    Flux<ImFriendshipPo> queryListByIds(String ownerId, List<String> ids);

    Mono<Boolean> creat(ImFriendshipPo friendship);

    Mono<Boolean> modify(ImFriendshipPo friendship);

    Mono<Boolean> removeOne(String ownerId, String friendId);
}
