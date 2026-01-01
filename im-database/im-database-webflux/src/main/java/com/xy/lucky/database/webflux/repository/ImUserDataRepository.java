package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImUserDataEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface ImUserDataRepository extends ReactiveCrudRepository<ImUserDataEntity, String> {
    Flux<ImUserDataEntity> findByUserIdIn(Collection<String> userIds);

    @Query("""
            select user_id, name, avatar, gender, birthday, location, extra
            from im_user_data
            where user_id = :keyword
            """)
    Flux<ImUserDataEntity> searchByKeyword(String keyword);
}
