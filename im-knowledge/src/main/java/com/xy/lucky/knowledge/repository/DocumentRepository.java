package com.xy.lucky.knowledge.repository;

import com.xy.lucky.knowledge.domain.po.DocumentPo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentRepository extends ReactiveCrudRepository<DocumentPo, Long> {
    Flux<DocumentPo> findByCreator(String creator);

    Flux<DocumentPo> findByGroupId(Long groupId);
}
