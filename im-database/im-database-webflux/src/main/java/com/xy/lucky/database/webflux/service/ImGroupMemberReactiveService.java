package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImGroupMemberEntity;
import com.xy.lucky.database.webflux.repository.ImGroupMemberRepository;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImGroupMemberReactiveService {

    private final ImGroupMemberRepository repository;

    public Flux<ImGroupMemberPo> queryList(String groupId) {
        return repository.findByGroupId(groupId).map(this::toPo);
    }

    public Mono<ImGroupMemberPo> queryOne(String groupId, String memberId) {
        return repository.findFirstByGroupIdAndMemberId(groupId, memberId).map(this::toPo);
    }

    public Mono<List<String>> queryNinePeopleAvatar(String groupId) {
        return repository.selectNinePeopleAvatar(groupId).collectList();
    }

    public Mono<Boolean> creat(ImGroupMemberPo groupMember) {
        return repository.save(fromPo(groupMember)).map(e -> true);
    }

    public Mono<Boolean> creatBatch(List<ImGroupMemberPo> groupMemberList) {
        return repository.saveAll(groupMemberList.stream().map(this::fromPo).toList())
                .count().map(count -> count == groupMemberList.size());
    }

    public Mono<Boolean> modify(ImGroupMemberPo groupMember) {
        return repository.save(fromPo(groupMember)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String memberId) {
        return repository.deleteById(memberId).thenReturn(true);
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

