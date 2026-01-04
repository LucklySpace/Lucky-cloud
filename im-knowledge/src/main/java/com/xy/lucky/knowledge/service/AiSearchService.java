package com.xy.lucky.knowledge.service;

import com.xy.lucky.knowledge.domain.vo.AiChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AiSearchService {

    /**
     * 索引文档
     */
    Mono<Void> indexDocument(Long docId, Integer version, byte[] content, String filename);

    /**
     * 删除索引
     */
    Mono<Void> deleteIndex(Long docId);

    /**
     * 语义搜索
     */
    Flux<String> search(String query, String creator);

    /**
     * AI 问答
     */
    Mono<AiChatResponse> chat(String query, String creator);
}
