package com.xy.lucky.knowledge.service.impl;

import com.xy.lucky.knowledge.domain.es.EsKnowledgeDoc;
import com.xy.lucky.knowledge.es.mapper.EsKnowledgeDocMapper;
import com.xy.lucky.knowledge.service.EsSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.elasticsearch.index.query.Operator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsSearchServiceImpl implements EsSearchService {

    private final EsKnowledgeDocMapper mapper;

    @Override
    public Mono<Void> indexText(EsKnowledgeDoc doc) {
        return Mono.fromRunnable(() -> {
            try {
                mapper.insert(doc);
            } catch (Exception e) {
                log.error("Index ES text failed, docId={}", doc.getDocId(), e);
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> deleteByDocId(Long docId) {
        return Mono.fromRunnable(() -> {
            try {
                LambdaEsQueryWrapper<EsKnowledgeDoc> wrapper = new LambdaEsQueryWrapper<>();
                wrapper.eq(EsKnowledgeDoc::getDocId, docId);
                mapper.delete(wrapper);
            } catch (Exception e) {
                log.error("Delete ES index by docId failed, docId={}", docId, e);
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Flux<EsKnowledgeDoc> searchText(String query, String creator, Long groupId) {
        return Mono.fromCallable(() -> {
                    LambdaEsQueryWrapper<EsKnowledgeDoc> wrapper = new LambdaEsQueryWrapper<>();
                    // 多字段匹配并按得分排序
                    wrapper.multiMatchQuery(query, Operator.OR, 60,
                            EsKnowledgeDoc::getTitle,
                            EsKnowledgeDoc::getContent
                    ).sortByScore();
                    if (creator != null && !creator.isEmpty()) {
                        wrapper.eq(EsKnowledgeDoc::getCreator, creator);
                    }
                    if (groupId != null) {
                        wrapper.eq(EsKnowledgeDoc::getGroupId, groupId);
                    }
                    return mapper.selectList(wrapper);
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }
}
