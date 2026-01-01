package com.xy.lucky.database.webflux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.database.webflux.entity.ImSingleMessageEntity;
import com.xy.lucky.database.webflux.repository.ImSingleMessageRepository;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImSingleMessageReactiveService {
    private final ImSingleMessageRepository repository;
    private final ObjectMapper objectMapper;

    public Mono<ImSingleMessagePo> queryOne(String messageId) {
        return repository.findById(messageId).map(this::toPo);
    }

    public Flux<ImSingleMessagePo> queryList(String userId, Long sequence) {
        return repository.findListByUserIdAndSequence(userId, sequence).map(this::toPo);
    }

    public Mono<ImSingleMessagePo> queryLast(String fromId, String toId) {
        return repository.findLastBetween(fromId, toId).map(this::toPo);
    }

    public Mono<Integer> queryReadStatus(String fromId, String toId, Integer code) {
        return repository.countReadStatus(fromId, toId, code);
    }

    public Mono<Boolean> creat(ImSingleMessagePo singleMessagePo) {
        return repository.save(fromPo(singleMessagePo)).map(e -> true);
    }

    public Mono<Boolean> creatBatch(List<ImSingleMessagePo> singleMessagePoList) {
        return repository.saveAll(singleMessagePoList.stream().map(this::fromPo).toList())
                .count().map(count -> count == singleMessagePoList.size());
    }

    public Mono<Boolean> modify(ImSingleMessagePo singleMessagePo) {
        return repository.save(fromPo(singleMessagePo)).map(e -> true);
    }

    public Mono<Boolean> removeOne(String messageId) {
        return repository.deleteById(messageId).thenReturn(true);
    }

    public Mono<Boolean> saveOrUpdate(ImSingleMessagePo messagePo) {
        return repository.save(fromPo(messagePo)).map(e -> true);
    }

    private ImSingleMessagePo toPo(ImSingleMessageEntity e) {
        ImSingleMessagePo p = new ImSingleMessagePo();
        p.setMessageId(e.getMessageId());
        p.setFromId(e.getFromId());
        p.setToId(e.getToId());
        if (e.getMessageBody() != null) {
            try {
                p.setMessageBody(objectMapper.readValue(e.getMessageBody(), Object.class));
            } catch (JsonProcessingException ex) {
                p.setMessageBody(e.getMessageBody());
            }
        }
        p.setMessageTime(e.getMessageTime());
        p.setMessageContentType(e.getMessageContentType());
        p.setReadStatus(e.getReadStatus());
        if (e.getExtra() != null) {
            try {
                p.setExtra(objectMapper.readValue(e.getExtra(), Object.class));
            } catch (JsonProcessingException ex) {
                p.setExtra(e.getExtra());
            }
        }
        if (e.getReplyMessage() != null) {
            try {
                p.setReplyMessage(objectMapper.readValue(e.getReplyMessage(), Object.class));
            } catch (JsonProcessingException ex) {
                p.setReplyMessage(e.getReplyMessage());
            }
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
            try {
                e.setMessageBody(objectMapper.writeValueAsString(p.getMessageBody()));
            } catch (JsonProcessingException ex) {
                e.setMessageBody(String.valueOf(p.getMessageBody()));
            }
        }
        e.setMessageTime(p.getMessageTime());
        e.setMessageContentType(p.getMessageContentType());
        e.setReadStatus(p.getReadStatus());
        if (p.getExtra() != null) {
            try {
                e.setExtra(objectMapper.writeValueAsString(p.getExtra()));
            } catch (JsonProcessingException ex) {
                e.setExtra(String.valueOf(p.getExtra()));
            }
        }
        if (p.getReplyMessage() != null) {
            try {
                e.setReplyMessage(objectMapper.writeValueAsString(p.getReplyMessage()));
            } catch (JsonProcessingException ex) {
                e.setReplyMessage(String.valueOf(p.getReplyMessage()));
            }
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
