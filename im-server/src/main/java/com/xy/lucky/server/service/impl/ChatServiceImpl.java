package com.xy.lucky.server.service.impl;

import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.mapper.ChatBeanMapper;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.domain.vo.ChatVo;
import com.xy.lucky.dubbo.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.api.database.user.ImUserDataDubboService;
import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.server.exception.ChatException;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.ChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    // 分布式锁常量
    private static final String LOCK_CREATE_CHAT_PREFIX = "lock:create:chat:";
    private static final String LOCK_READ_MSG_PREFIX = "lock:read:msg:";
    private static final String LOCK_READ_CHAT_PREFIX = "lock:read:chat:";
    private static final long LOCK_WAIT_TIME = 3L; // 锁等待时间（秒）
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有时间（秒）

    @DubboReference
    private ImChatDubboService imChatDubboService;

    @DubboReference
    private ImUserDataDubboService imUserDataDubboService;

    @DubboReference
    private ImGroupDubboService imGroupDubboService;

    @DubboReference
    private ImSingleMessageDubboService imSingleMessageDubboService;

    @DubboReference
    private ImGroupMessageDubboService imGroupMessageDubboService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 读消息（加锁防并发更新）
     */
    @Override
    public Mono<Void> read(ChatDto chatDto) {
        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null || chatDto.getChatType() == null) {
            log.warn("read参数无效");
            return Mono.error(new ChatException("参数错误"));
        }

        String lockKey = LOCK_READ_MSG_PREFIX + chatDto.getChatType() + ":" + chatDto.getFromId() + ":" + chatDto.getToId();

        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            IMessageType messageType = IMessageType.getByCode(chatDto.getChatType());
            if (messageType == null) {
                log.warn("未知chatType={}", chatDto.getChatType());
                return Mono.error(new ChatException("不支持的消息类型"));
            }

            return switch (messageType) {
                case SINGLE_MESSAGE -> saveSingleMessageChatReadStatus(chatDto)
                        .doOnSuccess(v -> log.debug("read消息耗时:{}ms", System.currentTimeMillis() - start));
                case GROUP_MESSAGE -> saveGroupMessageChatReadStatus(chatDto)
                        .doOnSuccess(v -> log.debug("read消息耗时:{}ms", System.currentTimeMillis() - start));
                default -> {
                    log.warn("未知chatType={}", chatDto.getChatType());
                    yield Mono.error(new ChatException("不支持的消息类型"));
                }
            };
        }), "设置消息已读 " + chatDto.getFromId() + "->" + chatDto.getToId());
    }

    /**
     * 创建会话（加锁防重复创建）
     */
    @Override
    public Mono<ChatVo> create(ChatDto chatDto) {
        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null || chatDto.getChatType() == null) {
            log.warn("create参数无效");
            return Mono.error(new ChatException("参数错误"));
        }

        String lockKey = LOCK_CREATE_CHAT_PREFIX + chatDto.getFromId() + ":" + chatDto.getToId() + ":" + chatDto.getChatType();
        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            return Mono.fromCallable(() -> imChatDubboService.queryOne(chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(imChatPO -> {
                        if (Objects.isNull(imChatPO)) {
                            ImChatPo newChatPO = new ImChatPo();
                            String chatId = UUID.randomUUID().toString();
                            newChatPO.setChatId(chatId)
                                    .setOwnerId(chatDto.getFromId())
                                    .setToId(chatDto.getToId())
                                    .setIsMute(IMStatus.NO.getCode())
                                    .setIsTop(IMStatus.NO.getCode())
                                    .setDelFlag(IMStatus.YES.getCode())
                                    .setChatType(chatDto.getChatType());

                            return Mono.fromCallable(() -> imChatDubboService.creat(newChatPO))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .flatMap(success -> {
                                        if (success) {
                                            log.info("会话信息插入成功 chatId={} from={} to={}", chatId, chatDto.getFromId(), chatDto.getToId());
                                            return Mono.just(newChatPO);
                                        } else {
                                            log.error("会话信息插入失败 chatId={} from={} to={}", chatId, chatDto.getFromId(), chatDto.getToId());
                                            return Mono.error(new GlobalException(ResultCode.FAIL, "创建会话失败"));
                                        }
                                    });
                        } else {
                            return Mono.just(imChatPO);
                        }
                    })
                    .flatMap(chatPo -> buildChatVo(chatPo, chatDto.getChatType()))
                    .doOnSuccess(v -> log.debug("create会话完成 from={} to={} type={} 耗时:{}ms", chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType(), System.currentTimeMillis() - start));
        }), "创建会话 " + chatDto.getFromId() + "->" + chatDto.getToId());
    }

    /**
     * 获取单个会话信息（加读锁）
     */
    @Override
    public Mono<ChatVo> one(String ownerId, String toId) {
        if (ownerId == null || toId == null) {
            log.warn("one参数无效");
            return Mono.error(new ChatException("参数错误"));
        }

        String lockKey = LOCK_READ_CHAT_PREFIX + ownerId + ":" + toId;
        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            return Mono.fromCallable(() -> imChatDubboService.queryOne(ownerId, toId, null))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(this::getChat)
                    .doOnSuccess(v -> log.debug("one会话完成 from={} to={} 耗时:{}ms", ownerId, toId, System.currentTimeMillis() - start));
        }), "读取会话 " + ownerId + "->" + toId);
    }

    /**
     * 获取会话列表（加读锁）
     */
    @Override
    public Mono<List<ChatVo>> list(ChatDto chatDto) {
        if (chatDto == null || chatDto.getFromId() == null) {
            log.warn("list参数无效");
            return Mono.error(new ChatException("参数错误"));
        }

        String lockKey = LOCK_READ_CHAT_PREFIX + chatDto.getFromId() + ":list";
        return withLock(lockKey, Mono.defer(() -> {
            long start = System.currentTimeMillis();
            return Mono.fromCallable(() -> imChatDubboService.queryList(chatDto.getFromId(), chatDto.getSequence()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMapMany(Flux::fromIterable)
                    .flatMap(this::getChat) // Concurrent processing
                    .collectList()
                    .doOnSuccess(result -> log.debug("list会话完成 from={} 返回{}条 耗时:{}ms", chatDto.getFromId(), result.size(), System.currentTimeMillis() - start));
        }), "读取会话列表 " + chatDto.getFromId());
    }

    /**
     * 获取ChatVo（提取公共逻辑）
     */
    private Mono<ChatVo> getChat(ImChatPo chatPo) {
        if (Objects.isNull(chatPo)) {
            return Mono.just(new ChatVo());
        }
        IMessageType type = IMessageType.getByCode(chatPo.getChatType());
        if (type == null) return Mono.just(new ChatVo());

        return switch (type) {
            case SINGLE_MESSAGE -> getSingleMessageChat(chatPo);
            case GROUP_MESSAGE -> getGroupMessageChat(chatPo);
            default -> Mono.just(new ChatVo());
        };
    }

    /**
     * 构建单聊ChatVo
     */
    private Mono<ChatVo> getSingleMessageChat(ImChatPo chatPo) {
        ChatVo chatVo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        String ownerId = chatVo.getOwnerId();
        String toId = chatVo.getToId();
        String targetUserId = ownerId.equals(toId) ? chatVo.getOwnerId() : chatVo.getToId();

        return Mono.zip(
                Mono.fromCallable(() -> imSingleMessageDubboService.queryLast(ownerId, toId)).subscribeOn(Schedulers.boundedElastic()).defaultIfEmpty(new ImSingleMessagePo()),
                Mono.fromCallable(() -> imSingleMessageDubboService.queryReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.getCode())).subscribeOn(Schedulers.boundedElastic()).defaultIfEmpty(0),
                Mono.fromCallable(() -> imUserDataDubboService.queryOne(targetUserId)).subscribeOn(Schedulers.boundedElastic()).defaultIfEmpty(new ImUserDataPo())
        ).map(tuple -> {
            ImSingleMessagePo msg = tuple.getT1();
            Integer unread = tuple.getT2();
            ImUserDataPo user = tuple.getT3();

            chatVo.setMessageTime(0L);
            if (msg.getMessageId() != null) { // Check if valid PO
                chatVo.setMessage(msg.getMessageBody());
                chatVo.setMessageContentType(msg.getMessageContentType());
                chatVo.setMessageTime(msg.getMessageTime());
            }

            chatVo.setUnread(unread);

            if (user.getUserId() != null) {
                chatVo.setName(user.getName());
                chatVo.setAvatar(user.getAvatar());
                chatVo.setId(user.getUserId());
            }
            return chatVo;
        });
    }

    /**
     * 构建群聊ChatVo
     */
    private Mono<ChatVo> getGroupMessageChat(ImChatPo chatPo) {
        ChatVo chatVo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        String ownerId = chatVo.getOwnerId();
        String groupId = chatVo.getToId();

        return Mono.zip(
                Mono.fromCallable(() -> imGroupMessageDubboService.queryLast(ownerId, groupId)).subscribeOn(Schedulers.boundedElastic()).defaultIfEmpty(new ImGroupMessagePo()),
                Mono.fromCallable(() -> imGroupMessageDubboService.queryReadStatus(groupId, ownerId, IMessageReadStatus.UNREAD.getCode())).subscribeOn(Schedulers.boundedElastic()).defaultIfEmpty(0),
                Mono.fromCallable(() -> imGroupDubboService.queryOne(groupId)).subscribeOn(Schedulers.boundedElastic()).defaultIfEmpty(new ImGroupPo())
        ).map(tuple -> {
            ImGroupMessagePo msg = tuple.getT1();
            Integer unread = tuple.getT2();
            ImGroupPo group = tuple.getT3();

            chatVo.setMessageTime(0L);
            if (msg.getMessageId() != null) {
                chatVo.setMessage(msg.getMessageBody());
                chatVo.setMessageTime(msg.getMessageTime());
            }

            chatVo.setUnread(unread);

            if (group.getGroupId() != null) {
                chatVo.setName(group.getGroupName());
                chatVo.setAvatar(group.getAvatar());
                chatVo.setId(group.getGroupId());
            }
            return chatVo;
        });
    }

    /**
     * 构建ChatVo（create专用）
     */
    private Mono<ChatVo> buildChatVo(ImChatPo chatPo, Integer chatType) {
        ChatVo chatVo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        if (IMessageType.SINGLE_MESSAGE.getCode().equals(chatType)) {
            return Mono.fromCallable(() -> imUserDataDubboService.queryOne(chatVo.getToId()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(user -> {
                        if (user != null) {
                            chatVo.setName(user.getName());
                            chatVo.setAvatar(user.getAvatar());
                            chatVo.setId(user.getUserId());
                        }
                        return chatVo;
                    });
        } else if (IMessageType.GROUP_MESSAGE.getCode().equals(chatType)) {
            return Mono.fromCallable(() -> imGroupDubboService.queryOne(chatVo.getToId()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(group -> {
                        if (group != null) {
                            chatVo.setName(group.getGroupName());
                            chatVo.setAvatar(group.getAvatar());
                            chatVo.setId(group.getGroupId());
                        }
                        return chatVo;
                    });
        }
        return Mono.just(chatVo);
    }

    private Mono<Void> saveSingleMessageChatReadStatus(ChatDto chatDto) {
        return Mono.fromCallable(() -> {
            ImSingleMessagePo updateMessage = new ImSingleMessagePo();
            updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
            updateMessage.setFromId(chatDto.getFromId());
            updateMessage.setToId(chatDto.getToId());
            return imSingleMessageDubboService.modify(updateMessage);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Void> saveGroupMessageChatReadStatus(ChatDto chatDto) {
        return Mono.fromCallable(() -> {
            ImGroupMessageStatusPo updateMessage = new ImGroupMessageStatusPo();
            updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
            updateMessage.setGroupId(chatDto.getFromId());
            updateMessage.setToId(chatDto.getToId());
            return imGroupMessageDubboService.creatBatch(List.of(updateMessage));
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private <T> Mono<T> withLock(String key, Mono<T> action, String logDesc) {
        RLockReactive lock = redissonClient.reactive().getLock(key);
        return lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)
                .flatMap(acquired -> {
                    if (!acquired) {
                        return Mono.error(new MessageException("无法获取锁: " + logDesc));
                    }
                    return action
                            .flatMap(res ->
                                    lock.isHeldByThread(Thread.currentThread().threadId())
                                            .flatMap(held -> held ? lock.unlock() : Mono.empty())
                                            .onErrorResume(e -> Mono.empty())
                                            .thenReturn(res)
                            )
                            .onErrorResume(e ->
                                    lock.isHeldByThread(Thread.currentThread().threadId())
                                            .flatMap(held -> held ? lock.unlock() : Mono.empty())
                                            .onErrorResume(unlockErr -> Mono.empty())
                                            .then(Mono.error(e))
                            );
                });
    }
}
