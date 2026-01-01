package com.xy.lucky.dubbo.webflux.api.database.user;

import com.xy.lucky.domain.po.ImUserDataPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImUserDataDubboWebfluxService {

    Mono<ImUserDataPo> queryOne(String userId);

    Mono<Boolean> creat(ImUserDataPo userDataPo);

    Mono<Boolean> creatBatch(List<ImUserDataPo> userDataPoList);

    Mono<Boolean> modify(ImUserDataPo userDataPo);

    Flux<ImUserDataPo> queryByKeyword(String keyword);

    Flux<ImUserDataPo> queryListByIds(List<String> userIdList);

    Mono<Boolean> removeOne(String userId);
}
