package com.xy.lucky.database.webflux.service;

import com.xy.lucky.database.webflux.entity.ImSingleMessageEntity;
import com.xy.lucky.database.webflux.repository.ImSingleMessageRepository;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import com.xy.lucky.dubbo.webflux.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.utils.json.JacksonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImSingleMessageReactiveService implements ImSingleMessageDubboService {
    private final ImSingleMessageRepository repository;

    @Override
    public Mono<ImSingleMessagePo> queryOne(String messageId) {
        return repository.findById(messageId).map(this::toPo);
    }

    @Override
    public Flux<ImSingleMessagePo> queryList(String userId, Long sequence) {
        return repository.findListByUserIdAndSequence(userId, sequence).map(this::toPo);
    }

    @Override
    public Mono<ImSingleMessagePo> queryLast(String fromId, String toId) {
        return repository.findLastBetween(fromId, toId).map(this::toPo);
    }

    @Override
    public Mono<Integer> queryReadStatus(String fromId, String toId, Integer code) {
        return repository.countReadStatus(fromId, toId, code);
    }

    @Override
    public Mono<Boolean> create(ImSingleMessagePo singleMessagePo) {
        return repository.save(fromPo(singleMessagePo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<ImSingleMessagePo> singleMessagePoList) {
        return repository.saveAll(singleMessagePoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == singleMessagePoList.size());
    }

    @Override
    public Mono<Boolean> modify(ImSingleMessagePo singleMessagePo) {
        return repository.save(fromPo(singleMessagePo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> removeOne(String messageId) {
        return repository.deleteById(messageId).thenReturn(true);
    }

    @Override
    public Mono<Boolean> saveOrUpdate(ImSingleMessagePo messagePo) {
        return repository.save(fromPo(messagePo)).map(e -> true);
    }

    private ImSingleMessagePo toPo(ImSingleMessageEntity e) {
        ImSingleMessagePo p = new ImSingleMessagePo();
        p.setMessageId(e.getMessageId());
        p.setFromId(e.getFromId());
        p.setToId(e.getToId());
        if (e.getMessageBody() != null) {
            p.setMessageBody(JacksonUtils.parseObject(e.getMessageBody(), Object.class));
        }
        p.setMessageTime(e.getMessageTime());
        p.setMessageContentType(e.getMessageContentType());
        p.setReadStatus(e.getReadStatus());
        if (e.getExtra() != null) {
            p.setExtra(JacksonUtils.parseObject(e.getExtra(), Object.class));
        }
        p.setDelFlag(e.getDelFlag());
        p.setSequence(e.getSequence());
        p.setMessageRandom(e.getMessageRandom());
        p.setCreateTime(e.getCreateTime());
        p.setUpdateTime(e.getUpdateTime());
        p.setVersion(e.getVersion());
        return p;
    }

    private ImSingleMessageEntity fromPo(ImSingleMessagePo p) {
        ImSingleMessageEntity e = new ImSingleMessageEntity();
        e.setMessageId(p.getMessageId());
        e.setFromId(p.getFromId());
        e.setToId(p.getToId());
        if (p.getMessageBody() != null) {
            e.setMessageBody(JacksonUtils.toJSONString(p.getMessageBody()));
        }
        e.setMessageTime(p.getMessageTime());
        e.setMessageContentType(p.getMessageContentType());
        e.setReadStatus(p.getReadStatus());
        if (p.getExtra() != null) {
            e.setExtra(JacksonUtils.toJSONString(p.getExtra()));
        }
        e.setDelFlag(p.getDelFlag());
        e.setSequence(p.getSequence());
        e.setMessageRandom(p.getMessageRandom());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setVersion(p.getVersion());
        return e;
    }
}
