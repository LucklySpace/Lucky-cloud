package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImUserEmojiPackEntity;
import com.xy.lucky.database.webflux.repository.ImUserEmojiPackRepository;
import com.xy.lucky.domain.po.ImUserEmojiPackPo;
import com.xy.lucky.dubbo.webflux.api.database.emoji.ImUserEmojiPackDubboWebfluxService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@DubboService
@RequiredArgsConstructor
public class ImUserEmojiPackReactiveService implements ImUserEmojiPackDubboWebfluxService {

    private final ImUserEmojiPackRepository repository;

    @Override
    public Flux<ImUserEmojiPackPo> listByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toPo);
    }

    @Override
    public Flux<String> listPackIds(String userId) {
        return repository.findByUserId(userId).map(ImUserEmojiPackEntity::getPackId);
    }

    @Override
    public Mono<Boolean> bindPack(String userId, String packId) {
        return repository.findByUserIdAndPackId(userId, packId)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(true);
                    }
                    ImUserEmojiPackEntity entity = new ImUserEmojiPackEntity();
                    entity.setId(UUID.randomUUID().toString());
                    entity.setUserId(userId);
                    entity.setPackId(packId);
                    entity.setCreateTime(System.currentTimeMillis());
                    entity.setUpdateTime(System.currentTimeMillis());
                    entity.setDelFlag(1);
                    entity.setVersion(1);
                    return repository.save(entity).map(e -> true);
                });
    }

    @Override
    public Mono<Boolean> bindPacks(String userId, List<String> packIds) {
        return Flux.fromIterable(packIds)
                .flatMap(packId -> bindPack(userId, packId))
                .all(b -> b);
    }

    @Override
    public Mono<Boolean> unbindPack(String userId, String packId) {
        return repository.findByUserIdAndPackId(userId, packId)
                .flatMap(entity -> {
                    entity.setDelFlag(0);
                    entity.setUpdateTime(System.currentTimeMillis());
                    return repository.save(entity);
                })
                .map(e -> true)
                .defaultIfEmpty(true); // If not found, considered unbound
    }

    private ImUserEmojiPackPo toPo(ImUserEmojiPackEntity e) {
        ImUserEmojiPackPo p = new ImUserEmojiPackPo();
        p.setId(e.getId());
        p.setUserId(e.getUserId());
        p.setPackId(e.getPackId());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }
}
