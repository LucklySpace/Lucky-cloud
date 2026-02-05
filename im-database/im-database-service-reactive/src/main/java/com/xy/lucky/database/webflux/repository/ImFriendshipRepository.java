package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImFriendshipEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ImFriendshipRepository extends ReactiveCrudRepository<ImFriendshipEntity, String> {
    @Query("""
            select *
            from im_friendship
            where owner_id = :ownerId
              and sequence > :sequence
            """)
    Flux<ImFriendshipEntity> selectFriendList(String ownerId, Long sequence);

    Mono<ImFriendshipEntity> findFirstByOwnerIdAndToId(String ownerId, String toId);

    @Query("""
            select *
            from im_friendship
            where owner_id = :ownerId
              and to_id in (:ids)
            """)
    Flux<ImFriendshipEntity> selectByOwnerIdAndToIds(String ownerId, Collection<String> ids);

    @Query("""
            update im_friendship
            set del_flag = 0, sequence = :seq
            where owner_id = :ownerId
              and to_id = :friendId
            """)
    Mono<Integer> softDelete(String ownerId, String friendId, Long seq);

    Mono<Integer> deleteByOwnerIdAndToId(String ownerId, String toId);
}

