package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipGroupEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipGroupRepository;
import com.xy.lucky.domain.po.ImFriendshipGroupPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImFriendshipGroupReactiveService {

    private final ImFriendshipGroupRepository repository;

    public Flux<ImFriendshipGroupPo> queryList(String ownerId) {
        return repository.findByFromId(ownerId).map(this::toPo);
    }

    public Mono<ImFriendshipGroupPo> queryOne(String id) {
        return repository.findById(id).map(this::toPo);
    }

    public Mono<Boolean> creat(ImFriendshipGroupPo friendshipGroupPo) {
        return repository.save(fromPo(friendshipGroupPo)).map(e -> true);
    }

    public Mono<Boolean> creatBatch(List<ImFriendshipGroupPo> friendshipGroupPoList) {
        return repository.saveAll(friendshipGroupPoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == friendshipGroupPoList.size());
    }

    public Mono<Boolean> modify(ImFriendshipGroupPo friendshipGroupPo) {
        return repository.save(fromPo(friendshipGroupPo)).map(e -> true);
    }

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

