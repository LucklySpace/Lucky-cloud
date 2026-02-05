package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImSingleMessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImSingleMessageRepository extends ReactiveCrudRepository<ImSingleMessageEntity, String> {
    @Query("select * from im_single_message where (from_id = :fromId and to_id = :toId) or (from_id = :toId and to_id = :fromId) order by message_time desc limit 1")
    Mono<ImSingleMessageEntity> findLastBetween(String fromId, String toId);

    @Query("select count(1) from im_single_message where from_id = :fromId and to_id = :toId and read_status = :status")
    Mono<Integer> countReadStatus(String fromId, String toId, Integer status);

    @Query("select * from im_single_message where (from_id = :userId or to_id = :userId) and message_time > :sequence order by message_time")
    Flux<ImSingleMessageEntity> findListByUserIdAndSequence(String userId, Long sequence);
}
