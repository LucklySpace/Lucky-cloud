package com.xy.lucky.server.service.impl;

import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.mapper.ChatBeanMapper;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.domain.vo.ChatVo;
import com.xy.lucky.dubbo.web.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.server.exception.ChatException;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.ChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
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
        return Mono.<Void>fromCallable(() -> {
                    if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null || chatDto.getChatType() == null) {
                        log.warn("read参数无效");
                        throw new ChatException("参数错误");
                    }

                    String lockKey = LOCK_READ_MSG_PREFIX + chatDto.getChatType() + ":" + chatDto.getFromId() + ":" + chatDto.getToId();
                    return withLockSync(lockKey, "设置消息已读 " + chatDto.getFromId() + "->" + chatDto.getToId(), () -> {
                        long start = System.currentTimeMillis();
                        IMessageType messageType = IMessageType.getByCode(chatDto.getChatType());
                        if (messageType == null) {
                            log.warn("未知chatType={}", chatDto.getChatType());
                            throw new ChatException("不支持的消息类型");
                        }

                        switch (messageType) {
                            case SINGLE_MESSAGE -> saveSingleMessageChatReadStatusSync(chatDto);
                            case GROUP_MESSAGE -> saveGroupMessageChatReadStatusSync(chatDto);
                            default -> throw new ChatException("不支持的消息类型");
                        }
                        log.debug("read消息耗时:{}ms", System.currentTimeMillis() - start);
                        return null;
                    });
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 创建会话（加锁防重复创建）
     */
    @Override
    public Mono<ChatVo> create(ChatDto chatDto) {
        return Mono.fromCallable(() -> {
                    if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null || chatDto.getChatType() == null) {
                        log.warn("create参数无效");
                        throw new ChatException("参数错误");
                    }

                    String lockKey = LOCK_CREATE_CHAT_PREFIX + chatDto.getFromId() + ":" + chatDto.getToId() + ":" + chatDto.getChatType();
                    return withLockSync(lockKey, "创建会话 " + chatDto.getFromId() + "->" + chatDto.getToId(), () -> {
                        long start = System.currentTimeMillis();

                        ImChatPo imChatPo = imChatDubboService.queryOne(chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType());
                        ImChatPo chatPo;
                        if (Objects.isNull(imChatPo)) {
                            ImChatPo newChatPO = new ImChatPo();
                            String chatId = UUID.randomUUID().toString();
                            newChatPO.setChatId(chatId)
                                    .setOwnerId(chatDto.getFromId())
                                    .setToId(chatDto.getToId())
                                    .setIsMute(IMStatus.NO.getCode())
                                    .setIsTop(IMStatus.NO.getCode())
                                    .setDelFlag(IMStatus.YES.getCode())
                                    .setChatType(chatDto.getChatType());

                            boolean success = imChatDubboService.creat(newChatPO);
                            if (success) {
                                log.info("会话信息插入成功 chatId={} from={} to={}", chatId, chatDto.getFromId(), chatDto.getToId());
                                chatPo = newChatPO;
                            } else {
                                log.error("会话信息插入失败 chatId={} from={} to={}", chatId, chatDto.getFromId(), chatDto.getToId());
                                throw new GlobalException(ResultCode.FAIL, "创建会话失败");
                            }
                        } else {
                            chatPo = imChatPo;
                        }

                        ChatVo vo = buildChatVoSync(chatPo, chatDto.getChatType());
                        log.debug("create会话完成 from={} to={} type={} 耗时:{}ms", vo.getOwnerId(), vo.getToId(), vo.getChatType(), System.currentTimeMillis() - start);
                        return vo;
                    });
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取单个会话信息（加读锁）
     */
    @Override
    public Mono<ChatVo> one(String ownerId, String toId) {
        return Mono.fromCallable(() -> {
                    if (ownerId == null || toId == null) {
                        log.warn("one参数无效");
                        throw new ChatException("参数错误");
                    }

                    String lockKey = LOCK_READ_CHAT_PREFIX + ownerId + ":" + toId;
                    return withLockSync(lockKey, "读取会话 " + ownerId + "->" + toId, () -> {
                        long start = System.currentTimeMillis();
                        ImChatPo chatPo = imChatDubboService.queryOne(ownerId, toId, null);
                        ChatVo vo = getChatSync(chatPo);
                        log.debug("one会话完成 from={} to={} 耗时:{}ms", ownerId, toId, System.currentTimeMillis() - start);
                        return vo;
                    });
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取会话列表（加读锁）
     */
    @Override
    public Mono<List<ChatVo>> list(ChatDto chatDto) {
        return Mono.fromCallable(() -> {
                    if (chatDto == null || chatDto.getFromId() == null) {
                        log.warn("list参数无效");
                        throw new ChatException("参数错误");
                    }

                    String lockKey = LOCK_READ_CHAT_PREFIX + chatDto.getFromId() + ":list";
                    return withLockSync(lockKey, "读取会话列表 " + chatDto.getFromId(), () -> {
                        long start = System.currentTimeMillis();

                        List<ImChatPo> list = imChatDubboService.queryList(chatDto.getFromId(), chatDto.getSequence());
                        if (list == null || list.isEmpty()) {
                            log.debug("list会话完成 from={} 返回0条 耗时:{}ms", chatDto.getFromId(), System.currentTimeMillis() - start);
                            return List.<ChatVo>of();
                        }

                        List<ChatVo> result = list.stream()
                                .map(this::getChatSync)
                                .toList();

                        log.debug("list会话完成 from={} 返回{}条 耗时:{}ms", chatDto.getFromId(), result.size(), System.currentTimeMillis() - start);
                        return result;
                    });
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取ChatVo（同步）
     */
    private ChatVo getChatSync(ImChatPo chatPo) {
        if (Objects.isNull(chatPo)) {
            return new ChatVo();
        }
        IMessageType type = IMessageType.getByCode(chatPo.getChatType());
        if (type == null) return new ChatVo();

        return switch (type) {
            case SINGLE_MESSAGE -> getSingleMessageChatSync(chatPo);
            case GROUP_MESSAGE -> getGroupMessageChatSync(chatPo);
            default -> new ChatVo();
        };
    }

    /**
     * 构建单聊ChatVo（同步）
     */
    private ChatVo getSingleMessageChatSync(ImChatPo chatPo) {
        ChatVo chatVo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        String ownerId = chatVo.getOwnerId();
        String toId = chatVo.getToId();
        String targetUserId = ownerId.equals(toId) ? chatVo.getOwnerId() : chatVo.getToId();

        ImSingleMessagePo msg = imSingleMessageDubboService.queryLast(ownerId, toId);
        Integer unread = imSingleMessageDubboService.queryReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.getCode());
        ImUserDataPo user = imUserDataDubboService.queryOne(targetUserId);

        chatVo.setMessageTime(0L);
        if (msg != null && msg.getMessageId() != null) {
            chatVo.setMessage(msg.getMessageBody());
            chatVo.setMessageContentType(msg.getMessageContentType());
            chatVo.setMessageTime(msg.getMessageTime());
        }

        chatVo.setUnread(unread != null ? unread : 0);

        if (user != null && user.getUserId() != null) {
            chatVo.setName(user.getName());
            chatVo.setAvatar(user.getAvatar());
            chatVo.setId(user.getUserId());
        }
        return chatVo;
    }

    /**
     * 构建群聊ChatVo（同步）
     */
    private ChatVo getGroupMessageChatSync(ImChatPo chatPo) {
        ChatVo chatVo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        String ownerId = chatVo.getOwnerId();
        String groupId = chatVo.getToId();

        ImGroupMessagePo msg = imGroupMessageDubboService.queryLast(ownerId, groupId);
        Integer unread = imGroupMessageDubboService.queryReadStatus(groupId, ownerId, IMessageReadStatus.UNREAD.getCode());
        ImGroupPo group = imGroupDubboService.queryOne(groupId);

        chatVo.setMessageTime(0L);
        if (msg != null && msg.getMessageId() != null) {
            chatVo.setMessage(msg.getMessageBody());
            chatVo.setMessageTime(msg.getMessageTime());
        }

        chatVo.setUnread(unread != null ? unread : 0);

        if (group != null && group.getGroupId() != null) {
            chatVo.setName(group.getGroupName());
            chatVo.setAvatar(group.getAvatar());
            chatVo.setId(group.getGroupId());
        }
        return chatVo;
    }

    /**
     * 构建ChatVo（create专用，同步）
     */
    private ChatVo buildChatVoSync(ImChatPo chatPo, Integer chatType) {
        ChatVo chatVo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        if (IMessageType.SINGLE_MESSAGE.getCode().equals(chatType)) {
            ImUserDataPo user = imUserDataDubboService.queryOne(chatVo.getToId());
            if (user != null) {
                chatVo.setName(user.getName());
                chatVo.setAvatar(user.getAvatar());
                chatVo.setId(user.getUserId());
            }
            return chatVo;
        } else if (IMessageType.GROUP_MESSAGE.getCode().equals(chatType)) {
            ImGroupPo group = imGroupDubboService.queryOne(chatVo.getToId());
            if (group != null) {
                chatVo.setName(group.getGroupName());
                chatVo.setAvatar(group.getAvatar());
                chatVo.setId(group.getGroupId());
            }
            return chatVo;
        }
        return chatVo;
    }

    private void saveSingleMessageChatReadStatusSync(ChatDto chatDto) {
        ImSingleMessagePo updateMessage = new ImSingleMessagePo();
        updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
        updateMessage.setFromId(chatDto.getFromId());
        updateMessage.setToId(chatDto.getToId());
        imSingleMessageDubboService.modify(updateMessage);
    }

    private void saveGroupMessageChatReadStatusSync(ChatDto chatDto) {
        ImGroupMessageStatusPo updateMessage = new ImGroupMessageStatusPo();
        updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
        updateMessage.setGroupId(chatDto.getFromId());
        updateMessage.setToId(chatDto.getToId());
        imGroupMessageDubboService.creatBatch(List.of(updateMessage));
    }

    private <T> T withLockSync(String key, String logDesc, java.util.concurrent.Callable<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new MessageException("无法获取锁: " + logDesc);
            }
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (acquired) {
                try {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                } catch (Exception ignore) {
                    // ignore unlock failures
                }
            }
        }
    }
}
