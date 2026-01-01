package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImGroupEntity;
import com.xy.lucky.database.webflux.repository.ImGroupRepository;
import com.xy.lucky.domain.po.ImGroupPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImGroupReactiveService {

    private final ImGroupRepository repository;

    public Flux<ImGroupPo> queryList(String userId) {
        return repository.selectGroupsByUserId(userId).map(this::toPo);
    }

    public Mono<ImGroupPo> queryOne(String groupId) {
        return repository.findById(groupId).map(this::toPo);
    }

    public Mono<Boolean> create(ImGroupPo groupPo) {
        return repository.save(fromPo(groupPo)).map(e -> true);
    }

    public Mono<Boolean> createBatch(List<ImGroupPo> list) {
        return repository.saveAll(list.stream().map(this::fromPo).toList())
                .count().map(count -> count == list.size());
    }

    public Mono<Boolean> modify(ImGroupPo groupPo) {
        return repository.save(fromPo(groupPo)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String groupId) {
        return repository.deleteById(groupId).thenReturn(true);
    }

    private ImGroupPo toPo(ImGroupEntity e) {
        ImGroupPo p = new ImGroupPo();
        p.setGroupId(e.getGroupId());
        p.setOwnerId(e.getOwnerId());
        p.setGroupType(e.getGroupType());
        p.setGroupName(e.getGroupName());
        p.setMute(e.getMute());
        p.setApplyJoinType(e.getApplyJoinType());
        p.setAvatar(e.getAvatar());
        p.setMaxMemberCount(e.getMaxMemberCount());
        p.setIntroduction(e.getIntroduction());
        p.setNotification(e.getNotification());
        p.setStatus(e.getStatus());
        p.setSequence(e.getSequence());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setExtra(e.getExtra());
        p.setVersion(e.getVersion());
        p.setDelFlag(e.getDelFlag());
        p.setMemberCount(e.getMemberCount());
        return p;
    }

    private ImGroupEntity fromPo(ImGroupPo p) {
        ImGroupEntity e = new ImGroupEntity();
        e.setGroupId(p.getGroupId());
        e.setOwnerId(p.getOwnerId());
        e.setGroupType(p.getGroupType());
        e.setGroupName(p.getGroupName());
        e.setMute(p.getMute());
        e.setApplyJoinType(p.getApplyJoinType());
        e.setAvatar(p.getAvatar());
        e.setMaxMemberCount(p.getMaxMemberCount());
        e.setIntroduction(p.getIntroduction());
        e.setNotification(p.getNotification());
        e.setStatus(p.getStatus());
        e.setSequence(p.getSequence());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setExtra(p.getExtra());
        e.setVersion(p.getVersion());
        e.setDelFlag(p.getDelFlag());
        e.setMemberCount(p.getMemberCount());
        return e;
    }
}

