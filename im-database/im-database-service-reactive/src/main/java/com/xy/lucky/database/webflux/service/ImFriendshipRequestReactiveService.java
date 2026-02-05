package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImFriendshipRequestEntity;
import com.xy.lucky.database.webflux.repository.ImFriendshipRequestRepository;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.database.rpc.api.database.friend.ImFriendshipRequestDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@DubboService
@RequiredArgsConstructor
public class ImFriendshipRequestReactiveService implements ImFriendshipRequestDubboService {

    private final ImFriendshipRequestRepository repository;

    @Override
    public Flux<ImFriendshipRequestPo> queryList(String userId) {
        return repository.findByToId(userId).map(this::toPo);
    }

    @Override
    public Mono<ImFriendshipRequestPo> queryOne(ImFriendshipRequestPo request) {
        if (request.getId() != null) {
            return repository.findById(request.getId()).map(this::toPo);
        }
        // Fallback or other query logic if needed
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> creat(ImFriendshipRequestPo request) {
        return repository.save(fromPo(request)).map(e -> true);
    }

    @Override
    public Mono<Boolean> modify(ImFriendshipRequestPo request) {
        return repository.findById(request.getId())
                .flatMap(e -> {
                    // Update fields
                    if (request.getReadStatus() != null) e.setReadStatus(request.getReadStatus());
                    if (request.getApproveStatus() != null) e.setApproveStatus(request.getApproveStatus());
                    if (request.getRemark() != null) e.setRemark(request.getRemark());
                    // ... other fields
                    e.setUpdateTime(System.currentTimeMillis());
                    return repository.save(e);
                })
                .map(e -> true)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Boolean> removeOne(String requestId) {
        return repository.deleteById(requestId).thenReturn(true);
    }

    @Override
    public Mono<Boolean> modifyStatus(String requestId, Integer status) {
        return repository.findById(requestId)
                .flatMap(e -> {
                    e.setApproveStatus(status);
                    e.setUpdateTime(System.currentTimeMillis());
                    return repository.save(e);
                })
                .map(e -> true)
                .defaultIfEmpty(false);
    }

    private ImFriendshipRequestPo toPo(ImFriendshipRequestEntity e) {
        ImFriendshipRequestPo p = new ImFriendshipRequestPo();
        p.setId(e.getId());
        p.setFromId(e.getFromId());
        p.setToId(e.getToId());
        p.setRemark(e.getRemark());
        p.setReadStatus(e.getReadStatus());
        p.setAddSource(e.getAddSource());
        p.setMessage(e.getMessage());
        p.setApproveStatus(e.getApproveStatus());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setSequence(e.getSequence());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImFriendshipRequestEntity fromPo(ImFriendshipRequestPo p) {
        ImFriendshipRequestEntity e = new ImFriendshipRequestEntity();
        e.setId(p.getId());
        e.setFromId(p.getFromId());
        e.setToId(p.getToId());
        e.setRemark(p.getRemark());
        e.setReadStatus(p.getReadStatus());
        e.setAddSource(p.getAddSource());
        e.setMessage(p.getMessage());
        e.setApproveStatus(p.getApproveStatus());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setSequence(p.getSequence());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}
