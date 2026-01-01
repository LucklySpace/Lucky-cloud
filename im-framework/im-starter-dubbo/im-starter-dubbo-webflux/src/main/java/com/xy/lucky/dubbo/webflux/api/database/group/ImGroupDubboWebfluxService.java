package com.xy.lucky.dubbo.webflux.api.database.group;

import com.xy.lucky.domain.po.ImGroupPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImGroupDubboWebfluxService {

    Flux<ImGroupPo> queryList(String userId);

    Mono<ImGroupPo> queryOne(String groupId);

    Mono<Boolean> creat(ImGroupPo groupPo);

    Mono<Boolean> modify(ImGroupPo groupPo);

    Mono<Boolean> creatBatch(List<ImGroupPo> list);

    Mono<Boolean> removeOne(String groupId);
}
