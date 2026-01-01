package com.xy.lucky.server.service.impl;

import com.xy.lucky.dubbo.web.api.database.emoji.ImUserEmojiPackDubboService;
import com.xy.lucky.server.service.UserEmojiService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
public class UserEmojiServiceImpl implements UserEmojiService {

    @DubboReference
    private ImUserEmojiPackDubboService dubboService;

    public Mono<List<String>> listPackIds(String userId) {
        return Mono.fromCallable(() -> dubboService.listPackIds(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> bindPack(String userId, String packId) {
        return Mono.fromCallable(() -> dubboService.bindPack(userId, packId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> unbindPack(String userId, String packId) {
        return Mono.fromCallable(() -> dubboService.unbindPack(userId, packId))
                .subscribeOn(Schedulers.boundedElastic());
    }
}

