package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImGroupMessageStatusEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ImGroupMessageStatusRepository extends ReactiveCrudRepository<ImGroupMessageStatusEntity, Long> {
    @Query("select count(1) from im_group_message_status where group_id = :groupId and to_id = :toId and read_status = :status")
    Mono<Integer> countReadStatus(String groupId, String toId, Integer status);

    @Query("select * from im_group_message_status where group_id = :groupId and message_id = :messageId and to_id = :toId")
    Mono<ImGroupMessageStatusEntity> findByGroupIdAndMessageIdAndToId(String groupId, String messageId, String toId);

    @Query("select count(1) from im_group_message_status where group_id = :groupId and message_id = :messageId and read_status = 1")
    Mono<Long> countRead(String groupId, String messageId);
}
