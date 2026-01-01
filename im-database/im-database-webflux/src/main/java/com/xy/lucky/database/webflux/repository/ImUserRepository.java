package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImUserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface ImUserRepository extends ReactiveCrudRepository<ImUserEntity, String> {
    Mono<ImUserEntity> findByMobile(String mobile);

    Flux<ImUserEntity> findByUserIdIn(Collection<String> userIds);

    @Query("select * from im_user where user_id = :userId limit 1")
    Mono<ImUserEntity> selectOne(String userId);
}
