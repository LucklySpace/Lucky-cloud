package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipGroupMemberEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipGroupMemberRepository;
import com.xy.lucky.domain.po.ImFriendshipGroupMemberPo;
import com.xy.lucky.database.rpc.api.database.friend.ImFriendshipGroupMemberDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImFriendshipGroupMemberReactiveService implements ImFriendshipGroupMemberDubboService {

    private final ImFriendshipGroupMemberRepository repository;

    @Override
    public Flux<ImFriendshipGroupMemberPo> queryList(String groupId) {
        return repository.findByGroupId(groupId).map(this::toPo);
    }

    @Override
    public Mono<ImFriendshipGroupMemberPo> queryOne(String groupId, String toId) {
        return repository.findByGroupIdAndToId(Long.valueOf(groupId), toId).map(this::toPo);
    }

    @Override
    public Mono<Boolean> create(ImFriendshipGroupMemberPo friendshipGroupMemberPo) {
        return repository.save(fromPo(friendshipGroupMemberPo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<ImFriendshipGroupMemberPo> friendshipGroupMemberPoList) {
        return repository.saveAll(friendshipGroupMemberPoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == friendshipGroupMemberPoList.size());
    }

    @Override
    public Mono<Boolean> modify(ImFriendshipGroupMemberPo friendshipGroupMemberPo) {
        return repository.save(fromPo(friendshipGroupMemberPo)).map(e -> true);
    }

    @Override
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

