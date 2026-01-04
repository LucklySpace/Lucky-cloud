package com.xy.lucky.knowledge.service;

import com.xy.lucky.knowledge.domain.es.EsKnowledgeDoc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EsSearchService {

    /**
     * 索引文档全文内容
     */
    Mono<Void> indexText(EsKnowledgeDoc doc);

    /**
     * 根据文档ID删除索引
     */
    Mono<Void> deleteByDocId(Long docId);

    /**
     * 全文检索
     */
    Flux<EsKnowledgeDoc> searchText(String query, String creator, Long groupId);
}
