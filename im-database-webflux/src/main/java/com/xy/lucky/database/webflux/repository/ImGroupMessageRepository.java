package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImGroupMessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImGroupMessageRepository extends ReactiveCrudRepository<ImGroupMessageEntity, String> {
    @Query("""
            select igm.*, igms.read_status
            from im_group_message igm
            inner join im_group_message_status igms
              on igm.message_id = igms.message_id and igm.group_id = igms.group_id
            where igms.group_id = :groupId
              and igms.to_id = :userId
              and igm.message_time > :sequence
            order by igm.message_time
            """)
    Flux<ImGroupMessageEntity> findListByUserIdAndSequence(String userId, String groupId, Long sequence);

    @Query("""
            select igm.*, igms.read_status
            from im_group_message igm
            inner join im_group_message_status igms
              on igm.message_id = igms.message_id and igm.group_id = igms.group_id
            where igms.group_id = :groupId
              and igms.to_id = :userId
            order by igm.message_time desc
            limit 1
            """)
    Mono<ImGroupMessageEntity> findLastByGroupIdAndUserId(String groupId, String userId);
}
