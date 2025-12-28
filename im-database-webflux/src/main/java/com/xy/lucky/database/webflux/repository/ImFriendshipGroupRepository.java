package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImFriendshipGroupEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImFriendshipGroupRepository extends ReactiveCrudRepository<ImFriendshipGroupEntity, String> {
    Flux<ImFriendshipGroupEntity> findByFromId(String fromId);
}

