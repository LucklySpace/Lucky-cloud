package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImGroupInviteRequestEntity;
import com.xy.lucky.database.webflux.repository.ImGroupInviteRequestRepository;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import com.xy.lucky.database.rpc.api.database.group.ImGroupInviteRequestDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImGroupInviteRequestReactiveService implements ImGroupInviteRequestDubboService {

    private final ImGroupInviteRequestRepository repository;

    @Override
    public Flux<ImGroupInviteRequestPo> queryList(String userId) {
        return repository.findByToId(userId).map(this::toPo);
    }

    @Override
    public Mono<ImGroupInviteRequestPo> queryOne(ImGroupInviteRequestPo po) {
        if (po.getRequestId() != null) {
            return repository.findById(po.getRequestId()).map(this::toPo);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> creat(ImGroupInviteRequestPo po) {
        return repository.save(fromPo(po)).map(e -> true);
    }

    @Override
    public Mono<Boolean> modify(ImGroupInviteRequestPo po) {
        return repository.findById(po.getRequestId())
                .flatMap(e -> {
                    if (po.getApproveStatus() != null) e.setApproveStatus(po.getApproveStatus());
                    if (po.getVerifierStatus() != null) e.setVerifierStatus(po.getVerifierStatus());
                    if (po.getVerifierId() != null) e.setVerifierId(po.getVerifierId());
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
    public Mono<Boolean> creatBatch(List<ImGroupInviteRequestPo> requests) {
        return repository.saveAll(requests.stream().map(this::fromPo).toList())
                .count()
                .map(count -> count == requests.size());
    }

    private ImGroupInviteRequestPo toPo(ImGroupInviteRequestEntity e) {
        ImGroupInviteRequestPo p = new ImGroupInviteRequestPo();
        p.setRequestId(e.getRequestId());
        p.setGroupId(e.getGroupId());
        p.setFromId(e.getFromId());
        p.setToId(e.getToId());
        p.setVerifierId(e.getVerifierId());
        p.setVerifierStatus(e.getVerifierStatus());
        p.setMessage(e.getMessage());
        p.setApproveStatus(e.getApproveStatus());
        p.setAddSource(e.getAddSource());
        p.setExpireTime(e.getExpireTime());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        return p;
    }

    private ImGroupInviteRequestEntity fromPo(ImGroupInviteRequestPo p) {
        ImGroupInviteRequestEntity e = new ImGroupInviteRequestEntity();
        e.setRequestId(p.getRequestId());
        e.setGroupId(p.getGroupId());
        e.setFromId(p.getFromId());
        e.setToId(p.getToId());
        e.setVerifierId(p.getVerifierId());
        e.setVerifierStatus(p.getVerifierStatus());
        e.setMessage(p.getMessage());
        e.setApproveStatus(p.getApproveStatus());
        e.setAddSource(p.getAddSource());
        e.setExpireTime(p.getExpireTime());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setDelFlag(p.getDelFlag());
        return e;
    }
}
