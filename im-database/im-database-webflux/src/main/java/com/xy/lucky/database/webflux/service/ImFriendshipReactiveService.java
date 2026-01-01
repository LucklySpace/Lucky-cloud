package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipRepository;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.dubbo.webflux.api.database.friend.ImFriendshipDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImFriendshipReactiveService implements ImFriendshipDubboService {

    private final ImFriendshipRepository repository;

    @Override
    public Mono<ImFriendshipPo> queryOne(String fromId, String toId) {
        return repository.findFirstByOwnerIdAndToId(fromId, toId).map(this::toPo);
    }

    @Override
    public Flux<ImFriendshipPo> queryList(String ownerId, Long sequence) {
        return repository.selectFriendList(ownerId, sequence).map(this::toPo);
    }

    @Override
    public Flux<ImFriendshipPo> queryListByIds(String ownerId, List<String> ids) {
        return repository.selectByOwnerIdAndToIds(ownerId, ids).map(this::toPo);
    }

    @Override
    public Mono<Boolean> create(ImFriendshipPo friendshipPo) {
        return repository.save(fromPo(friendshipPo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<ImFriendshipPo> friendshipPoList) {
        return repository.saveAll(friendshipPoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == friendshipPoList.size());
    }

    @Override
    public Mono<Boolean> modify(ImFriendshipPo friendshipPo) {
        return repository.save(fromPo(friendshipPo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> removeOne(String ownerId, String toId) {
        return repository.deleteByOwnerIdAndToId(ownerId, toId).thenReturn(true);
    }

    private ImFriendshipPo toPo(ImFriendshipEntity e) {
        ImFriendshipPo p = new ImFriendshipPo();
        p.setOwnerId(e.getOwnerId());
        p.setToId(e.getToId());
        p.setRemark(e.getRemark());
        p.setDelFlag(e.getDelFlag());
        p.setBlack(e.getBlack());
        p.setSequence(e.getSequence());
        p.setBlackSequence(e.getBlackSequence());
        p.setAddSource(e.getAddSource());
        p.setExtra(e.getExtra());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImFriendshipEntity fromPo(ImFriendshipPo p) {
        ImFriendshipEntity e = new ImFriendshipEntity();
        e.setOwnerId(p.getOwnerId());
        e.setToId(p.getToId());
        e.setRemark(p.getRemark());
        e.setDelFlag(p.getDelFlag());
        e.setBlack(p.getBlack());
        e.setSequence(p.getSequence());
        e.setBlackSequence(p.getBlackSequence());
        e.setAddSource(p.getAddSource());
        e.setExtra(p.getExtra());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setVersion(p.getVersion());
        return e;
    }
}

