package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImGroupMessageStatusEntity;
import com.xy.lucky.database.webflux.repository.ImGroupMessageStatusRepository;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import com.xy.lucky.database.rpc.api.database.message.ImGroupMessageStatusDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImGroupMessageStatusReactiveService implements ImGroupMessageStatusDubboService {

    private final ImGroupMessageStatusRepository repository;

    @Override
    public Mono<Boolean> saveOrUpdate(ImGroupMessageStatusPo statusPo) {
        return repository.findByGroupIdAndMessageIdAndToId(statusPo.getGroupId(), statusPo.getMessageId(), statusPo.getToId())
                .flatMap(entity -> {
                    // Update
                    entity.setReadStatus(statusPo.getReadStatus());
                    entity.setUpdateTime(System.currentTimeMillis());
                    return repository.save(entity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Create
                    ImGroupMessageStatusEntity entity = fromPo(statusPo);
                    entity.setCreateTime(System.currentTimeMillis());
                    entity.setUpdateTime(System.currentTimeMillis());
                    entity.setVersion(1);
                    return repository.save(entity);
                }))
                .map(e -> true);
    }

    @Override
    public Mono<Boolean> saveOrUpdateBatch(List<ImGroupMessageStatusPo> list) {
        return Flux.fromIterable(list)
                .flatMap(this::saveOrUpdate)
                .all(b -> b);
    }

    @Override
    public Mono<ImGroupMessageStatusPo> queryOne(String groupId, String messageId, String toId) {
        return repository.findByGroupIdAndMessageIdAndToId(groupId, messageId, toId)
                .map(this::toPo);
    }

    @Override
    public Mono<Long> countRead(String groupId, String messageId) {
        return repository.countRead(groupId, messageId);
    }

    private ImGroupMessageStatusPo toPo(ImGroupMessageStatusEntity e) {
        ImGroupMessageStatusPo p = new ImGroupMessageStatusPo();
        p.setGroupId(e.getGroupId());
        p.setMessageId(e.getMessageId());
        p.setToId(e.getToId());
        p.setReadStatus(e.getReadStatus());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImGroupMessageStatusEntity fromPo(ImGroupMessageStatusPo p) {
        ImGroupMessageStatusEntity e = new ImGroupMessageStatusEntity();
        e.setGroupId(p.getGroupId());
        e.setMessageId(p.getMessageId());
        e.setToId(p.getToId());
        e.setReadStatus(p.getReadStatus());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setVersion(p.getVersion());
        return e;
    }
}
