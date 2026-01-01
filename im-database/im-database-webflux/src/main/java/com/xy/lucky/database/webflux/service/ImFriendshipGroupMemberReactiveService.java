package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipGroupMemberEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipGroupMemberRepository;
import com.xy.lucky.domain.po.ImFriendshipGroupMemberPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImFriendshipGroupMemberReactiveService {

    private final ImFriendshipGroupMemberRepository repository;

    public Flux<ImFriendshipGroupMemberPo> queryList(String groupId) {
        return repository.findByGroupId(groupId).map(this::toPo);
    }

    public Mono<ImFriendshipGroupMemberPo> queryOne(String groupId, String memberId) {
        return repository.findFirstByGroupIdAndToId(groupId, memberId).map(this::toPo);
    }

    public Mono<Boolean> creat(ImFriendshipGroupMemberPo memberPo) {
        return repository.save(fromPo(memberPo)).map(e -> true);
    }

    public Mono<Boolean> creatBatch(List<ImFriendshipGroupMemberPo> memberPoList) {
        return repository.saveAll(memberPoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == memberPoList.size());
    }

    public Mono<Boolean> modify(ImFriendshipGroupMemberPo memberPo) {
        return repository.save(fromPo(memberPo)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String memberId) {
        return repository.deleteById(memberId).thenReturn(true);
    }

    private ImFriendshipGroupMemberPo toPo(ImFriendshipGroupMemberEntity e) {
        ImFriendshipGroupMemberPo p = new ImFriendshipGroupMemberPo();
        p.setGroupId(e.getGroupId());
        p.setToId(e.getToId());
        p.setCreateTime(e.getCreateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImFriendshipGroupMemberEntity fromPo(ImFriendshipGroupMemberPo p) {
        ImFriendshipGroupMemberEntity e = new ImFriendshipGroupMemberEntity();
        e.setGroupId(p.getGroupId());
        e.setToId(p.getToId());
        e.setCreateTime(p.getCreateTime());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}

