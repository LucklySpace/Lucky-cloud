package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImFriendshipGroupMemberEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImFriendshipGroupMemberRepository extends ReactiveCrudRepository<ImFriendshipGroupMemberEntity, String> {
    Flux<ImFriendshipGroupMemberEntity> findByGroupId(String groupId);

    Mono<ImFriendshipGroupMemberEntity> findFirstByGroupIdAndToId(String groupId, String toId);
}

