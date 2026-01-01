package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImGroupMessageStatusEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ImGroupMessageStatusRepository extends ReactiveCrudRepository<ImGroupMessageStatusEntity, Long> {
    @Query("select count(1) from im_group_message_status where group_id = :groupId and to_id = :toId and read_status = :status")
    Mono<Integer> countReadStatus(String groupId, String toId, Integer status);
}
