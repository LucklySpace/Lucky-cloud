package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImChatEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImChatRepository extends ReactiveCrudRepository<ImChatEntity, String> {
    Flux<ImChatEntity> findByOwnerIdAndSequenceGreaterThan(String ownerId, Long sequence);

    Mono<ImChatEntity> findFirstByOwnerIdAndToId(String ownerId, String toId);

    Mono<ImChatEntity> findFirstByOwnerIdAndToIdAndChatType(String ownerId, String toId, Integer chatType);
}
