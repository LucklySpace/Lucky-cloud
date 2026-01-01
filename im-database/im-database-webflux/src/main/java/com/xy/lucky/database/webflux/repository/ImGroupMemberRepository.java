package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImGroupMemberEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImGroupMemberRepository extends ReactiveCrudRepository<ImGroupMemberEntity, String> {
    Flux<ImGroupMemberEntity> findByGroupId(String groupId);

    Mono<ImGroupMemberEntity> findFirstByGroupIdAndMemberId(String groupId, String memberId);

    @Query("""
            SELECT iud.avatar
            FROM im_group_member igm
            JOIN im_user_data iud ON igm.member_id = iud.user_id
            WHERE igm.group_id = :groupId
            ORDER BY RANDOM() LIMIT 9
            """)
    Flux<String> selectNinePeopleAvatar(String groupId);
}

