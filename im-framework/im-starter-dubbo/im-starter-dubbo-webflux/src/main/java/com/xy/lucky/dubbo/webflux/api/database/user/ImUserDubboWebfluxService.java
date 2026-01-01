package com.xy.lucky.dubbo.webflux.api.database.user;

import com.xy.lucky.domain.po.ImUserPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImUserDubboWebfluxService {

    Flux<ImUserPo> queryList();

    Mono<ImUserPo> queryOne(String userId);

    Mono<Boolean> creat(ImUserPo userDataPo);

    Mono<Boolean> creatBatch(List<ImUserPo> userDataPoList);

    Mono<Boolean> modify(ImUserPo userDataPo);

    Mono<Boolean> removeOne(String userId);

    Mono<ImUserPo> queryOneByMobile(String phoneNumber);
}
