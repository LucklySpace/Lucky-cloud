package com.xy.lucky.dubbo.webflux.api.database.message;

import com.xy.lucky.domain.po.ImSingleMessagePo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImSingleMessageDubboWebfluxService {

    Flux<ImSingleMessagePo> queryList(String userId, Long sequence);

    Mono<ImSingleMessagePo> queryOne(String messageId);

    Mono<Boolean> creat(ImSingleMessagePo singleMessagePo);

    Mono<Boolean> creatBatch(List<ImSingleMessagePo> singleMessagePoList);

    Mono<Boolean> modify(ImSingleMessagePo singleMessagePo);

    Mono<Boolean> removeOne(String messageId);

    Mono<ImSingleMessagePo> queryLast(String fromId, String toId);

    Mono<Integer> queryReadStatus(String fromId, String toId, Integer code);
}
