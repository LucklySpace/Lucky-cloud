package com.xy.lucky.database.webflux.repository;

import com.xy.lucky.database.webflux.entity.ImUserEmojiPackEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ImUserEmojiPackRepository extends ReactiveCrudRepository<ImUserEmojiPackEntity, String> {

    @Query("select * from im_user_emoji_pack where user_id = :userId and del_flag = 1")
    Flux<ImUserEmojiPackEntity> findByUserId(String userId);

    @Query("select * from im_user_emoji_pack where user_id = :userId and pack_id = :packId and del_flag = 1")
    Mono<ImUserEmojiPackEntity> findByUserIdAndPackId(String userId, String packId);
}
