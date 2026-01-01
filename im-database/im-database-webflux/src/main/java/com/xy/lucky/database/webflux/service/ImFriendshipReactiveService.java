package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipRepository;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.utils.time.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImFriendshipReactiveService {

    private final ImFriendshipRepository repository;

    public Flux<ImFriendshipPo> queryList(String ownerId, Long sequence) {
        return repository.selectFriendList(ownerId, sequence).map(this::toPo);
    }

    public Flux<ImFriendshipPo> queryListByIds(String ownerId, List<String> ids) {
        return repository.selectByOwnerIdAndToIds(ownerId, ids).map(this::toPo);
    }

    public Mono<ImFriendshipPo> queryOne(String ownerId, String toId) {
        return repository.findFirstByOwnerIdAndToId(ownerId, toId).map(this::toPo);
    }

    public Mono<Boolean> creat(ImFriendshipPo friendship) {
        return repository.save(fromPo(friendship)).map(e -> true);
    }

    public Mono<Boolean> modify(ImFriendshipPo friendship) {
        return repository.save(fromPo(friendship)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String ownerId, String friendId) {
        return repository.softDelete(ownerId, friendId, DateTimeUtils.getCurrentUTCTimestamp())
                .map(rows -> rows > 0);
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

