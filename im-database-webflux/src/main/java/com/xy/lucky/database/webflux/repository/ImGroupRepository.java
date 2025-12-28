package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImGroupEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImGroupRepository extends ReactiveCrudRepository<ImGroupEntity, String> {
    @Query("""
            SELECT g.*, gm.member_count
            FROM im_group AS g
                     JOIN (SELECT group_id, COUNT(1) AS member_count
                           FROM im_group_member
                           GROUP BY group_id) AS gm ON gm.group_id = g.group_id
                     JOIN im_group_member AS m ON m.group_id = g.group_id
            WHERE m.member_id = :userId
            """)
    Flux<ImGroupEntity> selectGroupsByUserId(String userId);
}

