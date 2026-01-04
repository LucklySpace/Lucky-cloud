package com.xy.lucky.knowledge.service;

import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface CacheService {

    Mono<DocumentVo> getDocument(Long id);

    Mono<Void> setDocument(Long id, DocumentVo vo, Duration ttl);
}
