package com.xy.lucky.dubbo.webflux.api.database.message;

import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImGroupMessageDubboWebfluxService {

    Flux<ImGroupMessagePo> queryList(String groupId, Long sequence);

    Mono<ImGroupMessagePo> queryOne(String messageId);

    Mono<Boolean> creat(ImGroupMessagePo groupMessagePo);

    Mono<Boolean> creatBatch(List<ImGroupMessageStatusPo> groupMessagePoList);

    Mono<Boolean> modify(ImGroupMessagePo groupMessagePo);

    Mono<Boolean> removeOne(String messageId);

    Mono<ImGroupMessagePo> queryLast(String groupId, String userId);

    Mono<Integer> queryReadStatus(String groupId, String ownerId, Integer code);
}
