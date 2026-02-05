package com.xy.lucky.database.webflux.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.database.webflux.entity.ImGroupMessageEntity;
import com.xy.lucky.database.webflux.entity.ImGroupMessageStatusEntity;
import com.xy.lucky.database.webflux.repository.ImGroupMessageRepository;
import com.xy.lucky.database.webflux.repository.ImGroupMessageStatusRepository;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import com.xy.lucky.database.rpc.api.database.message.ImGroupMessageDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@DubboService
@RequiredArgsConstructor
public class ImGroupMessageReactiveService implements ImGroupMessageDubboService {
    private final ImGroupMessageRepository repository;
    private final ImGroupMessageStatusRepository statusRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<ImGroupMessagePo> queryList(String userId, Long sequence) {
        return repository.findListByUserIdAndSequence(userId, userId, sequence).map(this::toPo);
    }

    @Override
    public Mono<ImGroupMessagePo> queryOne(String messageId) {
        return repository.findById(messageId).map(this::toPo);
    }

    @Override
    public Mono<Boolean> create(ImGroupMessagePo groupMessagePo) {
        return repository.save(fromPo(groupMessagePo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> createBatch(List<ImGroupMessageStatusPo> groupMessagePoList) {
        return statusRepository.saveAll(groupMessagePoList.stream().map(this::fromStatusPo).toList())
                .count().map(count -> count == groupMessagePoList.size());
    }

    @Override
    public Mono<Boolean> modify(ImGroupMessagePo groupMessagePo) {
        return repository.save(fromPo(groupMessagePo)).map(e -> true);
    }

    @Override
    public Mono<Boolean> removeOne(String messageId) {
        return repository.deleteById(messageId).thenReturn(true);
    }

    @Override
    public Mono<ImGroupMessagePo> queryLast(String groupId, String userId) {
        return repository.findLastByGroupIdAndUserId(groupId, userId).map(this::toPo);
    }

    @Override
    public Mono<Integer> queryReadStatus(String groupId, String toId, Integer code) {
        return statusRepository.countReadStatus(groupId, toId, code);
    }

    private ImGroupMessagePo toPo(ImGroupMessageEntity e) {
        ImGroupMessagePo p = new ImGroupMessagePo();
        p.setMessageId(e.getMessageId());
        p.setGroupId(e.getGroupId());
        p.setFromId(e.getFromId());
        if (e.getMessageBody() != null) {
            try {
                p.setMessageBody(objectMapper.readValue(e.getMessageBody(), Object.class));
            } catch (JsonProcessingException ex) {
                p.setMessageBody(e.getMessageBody());
            }
        }
        p.setMessageTime(e.getMessageTime());
        p.setMessageContentType(e.getMessageContentType());
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
        p.setReadStatus(e.getReadStatus());
        return p;
    }

    private ImGroupMessageEntity fromPo(ImGroupMessagePo p) {
        ImGroupMessageEntity e = new ImGroupMessageEntity();
        e.setMessageId(p.getMessageId());
        e.setGroupId(p.getGroupId());
        e.setFromId(p.getFromId());
        if (p.getMessageBody() != null) {
            try {
                e.setMessageBody(objectMapper.writeValueAsString(p.getMessageBody()));
            } catch (JsonProcessingException ex) {
                e.setMessageBody(String.valueOf(p.getMessageBody()));
            }
        }
        e.setMessageTime(p.getMessageTime());
        e.setMessageContentType(p.getMessageContentType());
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
        e.setReadStatus(p.getReadStatus());
        return e;
    }

    private ImGroupMessageStatusEntity fromStatusPo(ImGroupMessageStatusPo p) {
        ImGroupMessageStatusEntity e = new ImGroupMessageStatusEntity();
        e.setGroupId(p.getGroupId());
        e.setMessageId(p.getMessageId());
        e.setToId(p.getToId());
        e.setReadStatus(p.getReadStatus());
        e.setCreateTime(p.getCreateTime());
        e.setUpdateTime(p.getUpdateTime());
        e.setVersion(p.getVersion());
        return e;
    }
}
