package com.xy.lucky.knowledge.service.impl;

import com.xy.lucky.knowledge.service.HeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatServiceImpl implements HeatService {

    private static final String KEY_ZSET = "doc:heat";

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> increaseHeat(Long docId, double score) {
        return redisTemplate.opsForZSet().incrementScore(KEY_ZSET, String.valueOf(docId), score)
                .map(s -> true)
                .onErrorReturn(false);
    }

    @Override
    public Flux<Long> topHotDocs(int topN) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(KEY_ZSET, Range.leftOpen(0L, topN + 1L))
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .onErrorResume(e -> {
                    log.error("Get top hot docs failed", e);
                    return Flux.empty();
                });
    }
}
