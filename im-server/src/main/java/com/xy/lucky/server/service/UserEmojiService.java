package com.xy.lucky.server.service;

import reactor.core.publisher.Mono;

import java.util.List;

public interface UserEmojiService {

    Mono<List<String>> listPackIds(String userId);

    Mono<Boolean> bindPack(String userId, String packId);

    Mono<Boolean> unbindPack(String userId, String packId);
}

