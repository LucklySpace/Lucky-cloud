package com.xy.lucky.dubbo.webflux.api.database.outbox;

import com.xy.lucky.domain.po.IMOutboxPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IMOutboxDubboWebfluxService {

    Flux<IMOutboxPo> queryList();

    Mono<IMOutboxPo> queryOne(Long id);

    Mono<Boolean> creat(IMOutboxPo outboxPo);

    Mono<Boolean> creatBatch(List<IMOutboxPo> list);

    Mono<Boolean> modify(IMOutboxPo outboxPo);

    Mono<Boolean> creatOrModify(IMOutboxPo outboxPo);

    Mono<Boolean> removeOne(Long id);

    Flux<IMOutboxPo> queryByStatus(String status, Integer limit);

    Mono<Boolean> modifyStatus(Long id, String status, Integer attempts);

    Mono<Boolean> modifyToFailed(Long id, String lastError, Integer attempts);
}
