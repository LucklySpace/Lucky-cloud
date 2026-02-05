package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipGroupEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipGroupRepository;
import com.xy.lucky.domain.po.ImFriendshipGroupPo;
import com.xy.lucky.database.rpc.api.database.friend.ImFriendshipGroupDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImFriendshipGroupReactiveService implements ImFriendshipGroupDubboService {

    private final ImFriendshipGroupRepository repository;

    @Override
    public Flux<ImFriendshipGroupPo> queryList(String id) {
        return repository.findByFromId(id).map(this::toPo);
    }

    @Override
    public Mono<ImFriendshipGroupPo> queryOne(String id) {
        return repository.findById(id).map(this::toPo);
    }

    @Override
    public Mono<Boolean> create(ImFriendshipGroupPo friendshipGroupPo) {
        return repository.save(fromPo(friendshipGroupPo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<ImFriendshipGroupPo> friendshipGroupPoList) {
        return repository.saveAll(friendshipGroupPoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == friendshipGroupPoList.size());
    }

    @Override
    public Mono<Boolean> modify(ImFriendshipGroupPo friendshipGroupPo) {
        return repository.save(fromPo(friendshipGroupPo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> removeOne(String id) {
        return repository.deleteById(id).thenReturn(true);
    }

    private ImFriendshipGroupPo toPo(ImFriendshipGroupEntity e) {
        ImFriendshipGroupPo p = new ImFriendshipGroupPo();
        p.setFromId(e.getFromId());
        p.setGroupId(e.getGroupId());
        p.setGroupName(e.getGroupName());
        p.setSequence(e.getSequence());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImFriendshipGroupEntity fromPo(ImFriendshipGroupPo p) {
        ImFriendshipGroupEntity e = new ImFriendshipGroupEntity();
        e.setFromId(p.getFromId());
        e.setGroupId(p.getGroupId());
        e.setGroupName(p.getGroupName());
        e.setSequence(p.getSequence());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}

