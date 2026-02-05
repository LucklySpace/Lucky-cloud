package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImFriendshipRequestEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImFriendshipRequestRepository extends ReactiveCrudRepository<ImFriendshipRequestEntity, String> {

    @Query("select * from im_friendship_request where to_id = :userId and del_flag = 1")
    Flux<ImFriendshipRequestEntity> findByToId(String userId);

    @Query("select * from im_friendship_request where from_id = :userId and del_flag = 1")
    Flux<ImFriendshipRequestEntity> findByFromId(String userId);
}
