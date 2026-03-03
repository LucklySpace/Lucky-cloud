package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImUserStickerPackEntity;
import com.xy.lucky.database.webflux.repository.ImUserStickerPackRepository;
import com.xy.lucky.domain.po.ImUserStickerPackPo;
import com.xy.lucky.database.rpc.api.database.sticker.ImUserStickerPackDubboWebfluxService;
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
public class ImUserStickerPackReactiveService implements ImUserStickerPackDubboWebfluxService {

    private final ImUserStickerPackRepository repository;

    @Override
    public Flux<ImUserStickerPackPo> listByUserId(String userId) {
        return repository.findByUserId(userId).map(this::toPo);
    }

    @Override
    public Flux<String> listPackIds(String userId) {
        return repository.findByUserId(userId).map(ImUserStickerPackEntity::getPackId);
    }

    @Override
    public Mono<Boolean> bindPack(String userId, String packId) {
        return repository.findByUserIdAndPackId(userId, packId)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(true);
                    }
                    ImUserStickerPackEntity entity = new ImUserStickerPackEntity();
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

    private ImUserStickerPackPo toPo(ImUserStickerPackEntity e) {
        ImUserStickerPackPo p = new ImUserStickerPackPo();
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
