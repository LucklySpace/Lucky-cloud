package com.xy.lucky.dubbo.webflux.api.database.emoji;

import com.xy.lucky.domain.po.ImUserEmojiPackPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ImUserEmojiPackDubboWebfluxService {

    Flux<ImUserEmojiPackPo> listByUserId(String userId);

    Flux<String> listPackIds(String userId);

    Mono<Boolean> bindPack(String userId, String packId);

    Mono<Boolean> bindPacks(String userId, List<String> packIds);

    Mono<Boolean> unbindPack(String userId, String packId);
}
