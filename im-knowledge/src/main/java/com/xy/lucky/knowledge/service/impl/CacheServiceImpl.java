package com.xy.lucky.knowledge.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.knowledge.domain.vo.DocumentVo;
import com.xy.lucky.knowledge.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private static final String KEY_PREFIX = "doc:detail:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<DocumentVo> getDocument(Long id) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + id)
                .flatMap(json -> {
                    try {
                        DocumentVo vo = objectMapper.readValue(json, DocumentVo.class);
                        return Mono.just(vo);
                    } catch (Exception e) {
                        log.warn("Deserialize cache failed, id={}", id, e);
                        return Mono.empty();
                    }
                });
    }

    @Override
    public Mono<Void> setDocument(Long id, DocumentVo vo, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            return redisTemplate.opsForValue().set(KEY_PREFIX + id, json, ttl).then();
        } catch (JsonProcessingException e) {
            return Mono.empty();
        }
    }
}
