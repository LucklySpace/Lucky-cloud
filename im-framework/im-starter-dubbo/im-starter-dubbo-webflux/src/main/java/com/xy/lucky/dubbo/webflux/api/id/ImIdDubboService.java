package com.xy.lucky.dubbo.webflux.api.id;

import com.xy.lucky.core.model.IMetaId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImIdDubboService {

    Mono<IMetaId> generateId(String type, String key);

    Flux<IMetaId> generateIds(String type, String key, Integer count);

    default <T> Mono<T> getId(String type, String key, Class<T> targetType) {
        return generateId(type, key).map(iMetaId -> targetType.cast(iMetaId.getMetaId()));
    }
}
