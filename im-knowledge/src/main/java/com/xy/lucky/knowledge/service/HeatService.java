package com.xy.lucky.knowledge.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HeatService {
    /**
     * 增加文档热度
     */
    Mono<Boolean> increaseHeat(Long docId, double score);

    /**
     * 获取热度最高的文档ID列表
     */
    Flux<Long> topHotDocs(int topN);
}
