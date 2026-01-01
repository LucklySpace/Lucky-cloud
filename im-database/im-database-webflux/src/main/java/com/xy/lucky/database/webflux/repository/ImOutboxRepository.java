package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImOutboxEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImOutboxRepository extends ReactiveCrudRepository<ImOutboxEntity, Long> {
    @Query("select * from im_outbox where status = :status limit :limit")
    Flux<ImOutboxEntity> findByStatusLimit(String status, int limit);
}
