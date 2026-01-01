package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImUserEntity;
import com.xy.lucky.database.webflux.repository.ImUserRepository;
import com.xy.lucky.domain.po.ImUserPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImUserReactiveService {
    private final ImUserRepository repository;

    public Mono<ImUserPo> queryOne(String userId) {
        return repository.findById(userId).map(this::toPo);
    }

    public Mono<ImUserPo> queryOneByMobile(String mobile) {
        return repository.findByMobile(mobile).map(this::toPo);
    }

    public Mono<Boolean> create(ImUserPo userPo) {
        return repository.save(fromPo(userPo)).map(e -> true);
    }

    public Mono<Boolean> createBatch(List<ImUserPo> userPoList) {
        return repository.saveAll(userPoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == userPoList.size());
    }

    public Mono<Boolean> modify(ImUserPo userPo) {
        return repository.save(fromPo(userPo)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String userId) {
        return repository.deleteById(userId).thenReturn(true);
    }

    public Flux<ImUserPo> listByIds(List<String> userIds) {
        return repository.findByUserIdIn(userIds).map(this::toPo);
    }

    public Mono<Long> count() {
        return repository.count();
    }

    private ImUserPo toPo(ImUserEntity e) {
        ImUserPo p = new ImUserPo();
        p.setUserId(e.getUserId());
        p.setUserName(e.getUserName());
        p.setPassword(e.getPassword());
        p.setMobile(e.getMobile());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImUserEntity fromPo(ImUserPo p) {
        ImUserEntity e = new ImUserEntity();
        e.setUserId(p.getUserId());
        e.setUserName(p.getUserName());
        e.setPassword(p.getPassword());
        e.setMobile(p.getMobile());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}
