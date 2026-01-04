package com.xy.lucky.knowledge.repository;

import com.xy.lucky.knowledge.domain.po.DocumentVersionPo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface DocumentVersionRepository extends ReactiveCrudRepository<DocumentVersionPo, Long> {
    Flux<DocumentVersionPo> findByDocumentId(Long documentId);
}
