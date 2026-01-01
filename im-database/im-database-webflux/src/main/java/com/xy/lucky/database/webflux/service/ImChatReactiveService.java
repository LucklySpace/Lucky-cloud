package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImChatEntity;
import com.xy.lucky.database.webflux.repository.ImChatRepository;
import com.xy.lucky.domain.po.ImChatPo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImChatReactiveService {
    private final ImChatRepository repository;

    public Flux<ImChatPo> queryList(String ownerId, Long sequence) {
        return repository.findByOwnerIdAndSequenceGreaterThan(ownerId, sequence).map(this::toPo);
    }

    public Mono<ImChatPo> queryOne(String ownerId, String toId, Integer chatType) {
        Mono<ImChatEntity> mono = chatType == null
                ? repository.findFirstByOwnerIdAndToId(ownerId, toId)
                : repository.findFirstByOwnerIdAndToIdAndChatType(ownerId, toId, chatType);
        return mono.map(this::toPo);
    }

    public Mono<Boolean> creat(ImChatPo chatPo) {
        return repository.save(fromPo(chatPo)).map(e -> true);
    }

    public Mono<Boolean> modify(ImChatPo chatPo) {
        return repository.save(fromPo(chatPo)).map(e -> true);
    }

    public Mono<Boolean> saveOrUpdate(ImChatPo chatPo) {
        return repository.save(fromPo(chatPo)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String id) {
        return repository.deleteById(id).thenReturn(true);
    }

    private ImChatPo toPo(ImChatEntity e) {
        ImChatPo p = new ImChatPo();
        p.setChatId(e.getChatId());
        p.setChatType(e.getChatType());
        p.setOwnerId(e.getOwnerId());
        p.setToId(e.getToId());
        p.setIsMute(e.getIsMute());
        p.setIsTop(e.getIsTop());
        p.setSequence(e.getSequence());
        p.setReadSequence(e.getReadSequence());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setDelFlag(e.getDelFlag());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImChatEntity fromPo(ImChatPo p) {
        ImChatEntity e = new ImChatEntity();
        e.setChatId(p.getChatId());
        e.setChatType(p.getChatType());
        e.setOwnerId(p.getOwnerId());
        e.setToId(p.getToId());
        e.setIsMute(p.getIsMute());
        e.setIsTop(p.getIsTop());
        e.setSequence(p.getSequence());
        e.setReadSequence(p.getReadSequence());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setDelFlag(p.getDelFlag());
        e.setVersion(p.getVersion());
        return e;
    }
}
