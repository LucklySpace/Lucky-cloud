package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImGroupInviteRequestEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImGroupInviteRequestRepository extends ReactiveCrudRepository<ImGroupInviteRequestEntity, String> {

    @Query("select * from im_group_invite_request where to_id = :userId and del_flag = 1")
    Flux<ImGroupInviteRequestEntity> findByToId(String userId);

    @Query("select * from im_group_invite_request where group_id = :groupId and del_flag = 1")
    Flux<ImGroupInviteRequestEntity> findByGroupId(String groupId);
}
