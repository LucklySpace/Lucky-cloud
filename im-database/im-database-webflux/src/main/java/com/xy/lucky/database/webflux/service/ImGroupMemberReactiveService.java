package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImGroupMemberEntity;
import com.xy.lucky.database.webflux.repository.ImGroupMemberRepository;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.dubbo.webflux.api.database.group.ImGroupMemberDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImGroupMemberReactiveService implements ImGroupMemberDubboService {

    private final ImGroupMemberRepository repository;

    @Override
    public Flux<ImGroupMemberPo> queryList(String groupId) {
        return repository.findByGroupId(groupId).map(this::toPo);
    }

    @Override
    public Mono<ImGroupMemberPo> queryOne(String groupId, String memberId) {
        return repository.findFirstByGroupIdAndMemberId(groupId, memberId).map(this::toPo);
    }

    @Override
    public Flux<ImGroupMemberPo> queryByRole(String groupId, Integer role) {
        return repository.findByGroupIdAndRole(groupId, role).map(this::toPo);
    }

    @Override
    public Mono<List<String>> queryNinePeopleAvatar(String groupId) {
        return repository.selectNinePeopleAvatar(groupId).collectList();
    }

    @Override
    public Mono<Boolean> create(ImGroupMemberPo groupMember) {
        return repository.save(fromPo(groupMember)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<ImGroupMemberPo> groupMemberList) {
        return repository.saveAll(groupMemberList.stream().map(this::fromPo).toList())
                .count().map(count -> count == groupMemberList.size());
    }

    @Override
    public Mono<Boolean> modify(ImGroupMemberPo groupMember) {
        return repository.save(fromPo(groupMember)).map(e -> true);
    }

    @Override
    public Mono<Boolean> modifyBatch(List<ImGroupMemberPo> groupMemberList) {
        return repository.saveAll(groupMemberList.stream().map(this::fromPo).toList())
                .count().map(count -> count == groupMemberList.size());
    }

    @Override
    public Mono<Boolean> removeOne(String memberId) {
        return repository.deleteById(memberId).thenReturn(true);
    }

    @Override
    public Mono<Boolean> removeByGroupId(String groupId) {
        return repository.deleteByGroupId(groupId).thenReturn(true);
    }

    @Override
    public Mono<Long> countByGroupId(String groupId) {
        return repository.countByGroupId(groupId);
    }

    private ImGroupMemberPo toPo(ImGroupMemberEntity e) {
        ImGroupMemberPo p = new ImGroupMemberPo();
        p.setGroupMemberId(e.getGroupMemberId());
        p.setGroupId(e.getGroupId());
        p.setMemberId(e.getMemberId());
        p.setRole(e.getRole());
        p.setSpeakDate(e.getSpeakDate());
        p.setMute(e.getMute());
        p.setAlias(e.getAlias());
        p.setJoinTime(e.getJoinTime());
        p.setLeaveTime(e.getLeaveTime());
        p.setJoinType(e.getJoinType());
        p.setRemark(e.getRemark());
        p.setExtra(e.getExtra());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImGroupMemberEntity fromPo(ImGroupMemberPo p) {
        ImGroupMemberEntity e = new ImGroupMemberEntity();
        e.setGroupMemberId(p.getGroupMemberId());
        e.setGroupId(p.getGroupId());
        e.setMemberId(p.getMemberId());
        e.setRole(p.getRole());
        e.setSpeakDate(p.getSpeakDate());
        e.setMute(p.getMute());
        e.setAlias(p.getAlias());
        e.setJoinTime(p.getJoinTime());
        e.setLeaveTime(p.getLeaveTime());
        e.setJoinType(p.getJoinType());
        e.setRemark(p.getRemark());
        e.setExtra(p.getExtra());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}

