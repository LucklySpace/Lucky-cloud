package com.xy.lucky.database.webflux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.database.webflux.entity.ImOutboxEntity;
import com.xy.lucky.database.webflux.repository.ImOutboxRepository;
import com.xy.lucky.domain.po.IMOutboxPo;
import com.xy.lucky.database.rpc.api.database.outbox.ImOutboxDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImOutboxReactiveService implements ImOutboxDubboService {
    private final ImOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<IMOutboxPo> queryList() {
        return repository.findAll().map(this::toPo);
    }

    @Override
    public Mono<IMOutboxPo> queryOne(Long id) {
        return repository.findById(id).map(this::toPo);
    }

    @Override
    public Mono<Boolean> create(IMOutboxPo po) {
        return repository.save(fromPo(po)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<IMOutboxPo> list) {
        return repository.saveAll(list.stream().map(this::fromPo).toList())
                .count().map(count -> count == list.size());
    }

    @Override
    public Mono<Boolean> modify(IMOutboxPo po) {
        return repository.save(fromPo(po)).map(e -> true);
    }

    @Override
    public Mono<Boolean> removeOne(Long id) {
        return repository.deleteById(id).thenReturn(true);
    }

    @Override
    public Flux<IMOutboxPo> queryByStatus(String status, Integer limit) {
        String s = status == null ? null : status.trim().toUpperCase();
        int lim = limit == null ? 100 : Math.max(1, Math.min(limit, 1000));
        return repository.findByStatusLimit(s, lim).map(this::toPo);
    }

    @Override
    public Mono<Boolean> modifyStatus(Long id, String status, Integer attempts) {
        return repository.findById(id)
                .flatMap(e -> {
                    e.setStatus(status == null ? null : status.trim().toUpperCase());
                    e.setAttempts(attempts == null ? 0 : Math.max(0, attempts));
                    return repository.save(e);
                })
                .map(saved -> true);
    }

    @Override
    public Mono<Boolean> modifyToFailed(Long id, String lastError, Integer attempts) {
        return repository.findById(id)
                .flatMap(e -> {
                    String err = lastError == null ? null : (lastError.length() > 1024 ? lastError.substring(0, 1024) : lastError);
                    e.setLastError(err);
                    e.setAttempts(attempts == null ? 0 : Math.max(0, attempts));
                    return repository.save(e);
                })
                .map(saved -> true);
    }

    private IMOutboxPo toPo(ImOutboxEntity e) {
        IMOutboxPo p = new IMOutboxPo();
        p.setId(e.getId());
        p.setMessageId(e.getMessageId());
        p.setExchange(e.getExchange());
        p.setRoutingKey(e.getRoutingKey());
        p.setAttempts(e.getAttempts());
        p.setStatus(e.getStatus());
        p.setLastError(e.getLastError());
        p.setNextTryAt(e.getNextTryAt());
        p.setCreatedAt(e.getCreatedAt());
        p.setUpdatedAt(e.getUpdatedAt());
        if (e.getPayload() != null) {
            try {
                p.setPayload(objectMapper.readValue(e.getPayload(), Object.class));
            } catch (JsonProcessingException ex) {
                p.setPayload(e.getPayload());
            }
        }
        return p;
    }

    private ImOutboxEntity fromPo(IMOutboxPo p) {
        ImOutboxEntity e = new ImOutboxEntity();
        e.setId(p.getId());
        e.setMessageId(p.getMessageId());
        e.setExchange(p.getExchange());
        e.setRoutingKey(p.getRoutingKey());
        e.setAttempts(p.getAttempts());
        e.setStatus(p.getStatus());
        e.setLastError(p.getLastError());
        e.setNextTryAt(p.getNextTryAt());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        if (p.getPayload() != null) {
            try {
                e.setPayload(objectMapper.writeValueAsString(p.getPayload()));
            } catch (JsonProcessingException ex) {
                e.setPayload(String.valueOf(p.getPayload()));
            }
        }
        return e;
    }
}
