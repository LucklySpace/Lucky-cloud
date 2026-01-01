package com.xy.lucky.dubbo.webflux.api.database.chat;

import com.xy.lucky.domain.po.ImChatPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImChatDubboWebfluxService {

    Mono<ImChatPo> queryOne(String ownerId, String toId, Integer chatType);

    Flux<ImChatPo> queryList(String ownerId, Long sequence);

    Mono<Boolean> creat(ImChatPo chatPo);

    Mono<Boolean> modify(ImChatPo chatPo);

    Mono<Boolean> creatOrModify(ImChatPo chatPo);

    Mono<Boolean> removeOne(String id);
}
