package com.xy.server.service.impl;

import com.xy.core.enums.IMStatus;
import com.xy.core.enums.IMessageReadStatus;
import com.xy.core.enums.IMessageType;
import com.xy.domain.dto.ChatDto;
import com.xy.domain.po.*;
import com.xy.domain.vo.ChatVo;
import com.xy.dubbo.api.database.chat.ImChatDubboService;
import com.xy.dubbo.api.database.group.ImGroupDubboService;
import com.xy.dubbo.api.database.message.ImGroupMessageDubboService;
import com.xy.dubbo.api.database.message.ImSingleMessageDubboService;
import com.xy.dubbo.api.database.user.ImUserDataDubboService;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.exception.GlobalException;
import com.xy.server.service.ChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    /**
     * 读消息（加锁防并发更新）
     */
    @Override
    public Result read(ChatDto chatDto) {
        long start = System.currentTimeMillis();
        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null || chatDto.getChatType() == null) {
            log.warn("read参数无效");
            return Result.failed("参数错误");
        }

        String lockKey = LOCK_READ_MSG_PREFIX + chatDto.getChatType() + ":" + chatDto.getFromId() + ":" + chatDto.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取read消息锁 chatType={} from={} to={}", chatDto.getChatType(), chatDto.getFromId(), chatDto.getToId());
                return Result.failed("消息处理中，请稍后重试");
            }

            return switch (Objects.requireNonNull(IMessageType.getByCode(chatDto.getChatType()))) {
                case SINGLE_MESSAGE -> saveSingleMessageChatReadStatus(chatDto);
                case GROUP_MESSAGE -> saveGroupMessageChatReadStatus(chatDto);
                default -> {
                    log.warn("未知chatType={}", chatDto.getChatType());
                    yield Result.failed("不支持的消息类型");
                }
            };
        } catch (Exception e) {
            log.error("read消息异常 chatType={} from={} to={}", chatDto.getChatType(), chatDto.getFromId(), chatDto.getToId(), e);
            throw new GlobalException(ResultCode.FAIL, "设置消息已读失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.debug("read消息耗时:{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 创建会话（加锁防重复创建）
     */
    @Override
    public ChatVo create(ChatDto chatDto) {
        long start = System.currentTimeMillis();
        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null || chatDto.getChatType() == null) {
            log.warn("create参数无效");
            throw new GlobalException(ResultCode.FAIL, "参数错误");
        }

        String lockKey = LOCK_CREATE_CHAT_PREFIX + chatDto.getFromId() + ":" + chatDto.getToId() + ":" + chatDto.getChatType();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取create会话锁 from={} to={} type={}", chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType());
                throw new GlobalException(ResultCode.FAIL, "会话创建中，请稍后重试");
            }

            ImChatPo imChatPO = imChatDubboService.selectOne(chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType());
            if (Objects.isNull(imChatPO)) {
                imChatPO = new ImChatPo();
                String chatId = UUID.randomUUID().toString();
                imChatPO.setChatId(chatId)
                        .setOwnerId(chatDto.getFromId())
                        .setToId(chatDto.getToId())
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setDelFlag(IMStatus.YES.getCode())
                        .setChatType(chatDto.getChatType());

                boolean success = imChatDubboService.insert(imChatPO);
                if (success) {
                    log.info("会话信息插入成功 chatId={} from={} to={}", chatId, chatDto.getFromId(), chatDto.getToId());
                } else {
                    log.error("会话信息插入失败 chatId={} from={} to={}", chatId, chatDto.getFromId(), chatDto.getToId());
                    throw new GlobalException(ResultCode.FAIL, "创建会话失败");
                }
            }

            ChatVo chatVo = buildChatVo(imChatPO, chatDto.getChatType());
            log.debug("create会话完成 from={} to={} type={} 耗时:{}ms", chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType(), System.currentTimeMillis() - start);
            return chatVo;
        } catch (Exception e) {
            log.error("create会话异常 from={} to={} type={}", chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType(), e);
            throw new GlobalException(ResultCode.FAIL, "创建会话失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取单个会话信息（加读锁）
     */
    @Override
    public ChatVo one(String ownerId, String toId) {
        long start = System.currentTimeMillis();
        if (ownerId == null || toId == null) {
            log.warn("one参数无效");
            throw new GlobalException(ResultCode.FAIL, "参数错误");
        }

        String lockKey = LOCK_READ_CHAT_PREFIX + ownerId + ":" + toId;
        RLock readLock = redissonClient.getLock(lockKey);
        try {
            if (!readLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取one会话读锁 from={} to={}", ownerId, toId);
                throw new GlobalException(ResultCode.FAIL, "会话读取中，请稍后重试");
            }

            ImChatPo imChatPO = imChatDubboService.selectOne(ownerId, toId, null);
            ChatVo chatVo = getChat(imChatPO);
            log.debug("one会话完成 from={} to={} 耗时:{}ms", ownerId, toId, System.currentTimeMillis() - start);
            return chatVo;
        } catch (Exception e) {
            log.error("one会话异常 from={} to={}", ownerId, toId, e);
            throw new GlobalException(ResultCode.FAIL, "获取会话失败");
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 获取会话列表（加读锁）
     */
    @Override
    public List<ChatVo> list(ChatDto chatDto) {
        long start = System.currentTimeMillis();
        if (chatDto == null || chatDto.getFromId() == null) {
            log.warn("list参数无效");
            throw new GlobalException(ResultCode.FAIL, "参数错误");
        }

        String lockKey = LOCK_READ_CHAT_PREFIX + chatDto.getFromId() + ":list";
        RLock readLock = redissonClient.getLock(lockKey);
        try {
            if (!readLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取list会话读锁 from={}", chatDto.getFromId());
                throw new GlobalException(ResultCode.FAIL, "会话列表读取中，请稍后重试");
            }

            List<ImChatPo> imChatPos = imChatDubboService.selectList(chatDto.getFromId(), chatDto.getSequence());
            List<CompletableFuture<ChatVo>> chatFutures = imChatPos.stream()
                    .map(e -> CompletableFuture.supplyAsync(() -> getChat(e), asyncTaskExecutor))
                    .toList();

            List<ChatVo> result = chatFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            log.debug("list会话完成 from={} 返回{}条 耗时:{}ms", chatDto.getFromId(), result.size(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("list会话异常 from={}", chatDto.getFromId(), e);
            throw new GlobalException(ResultCode.FAIL, "获取会话列表失败");
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 获取ChatVo（提取公共逻辑）
     */
    private ChatVo getChat(ImChatPo chatPo) {
        if (chatPo == null) {
            return new ChatVo();
        }
        switch (IMessageType.getByCode(chatPo.getChatType())) {
            case SINGLE_MESSAGE:
                return getSingleMessageChat(chatPo);
            case GROUP_MESSAGE:
                return getGroupMessageChat(chatPo);
            default:
                log.warn("未知chatType={}", chatPo.getChatType());
                return new ChatVo();
        }
    }

    /**
     * 构建单聊ChatVo
     */
    private ChatVo getSingleMessageChat(ImChatPo chatPo) {
        ChatVo chatVo = new ChatVo();
        BeanUtils.copyProperties(chatPo, chatVo);

        String ownerId = chatVo.getOwnerId();
        String toId = chatVo.getToId();

        ImSingleMessagePo singleMessageDto = imSingleMessageDubboService.last(ownerId, toId);

        chatVo.setMessageTime(0L);
        if (Objects.nonNull(singleMessageDto)) {
            chatVo.setMessage(singleMessageDto.getMessageBody());
            chatVo.setMessageContentType(singleMessageDto.getMessageContentType());
            chatVo.setMessageTime(singleMessageDto.getMessageTime());
        }

        Integer unread = imSingleMessageDubboService.selectReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.getCode());
        chatVo.setUnread(Objects.requireNonNullElse(unread, 0));

        String targetUserId = ownerId.equals(toId) ? chatVo.getOwnerId() : chatVo.getToId();
        ImUserDataPo imUserDataPo = imUserDataDubboService.selectOne(targetUserId);
        if (imUserDataPo != null) {
            chatVo.setName(imUserDataPo.getName());
            chatVo.setAvatar(imUserDataPo.getAvatar());
            chatVo.setId(imUserDataPo.getUserId());
        }

        return chatVo;
    }

    /**
     * 构建群聊ChatVo
     */
    private ChatVo getGroupMessageChat(ImChatPo chatPo) {
        ChatVo chatVo = new ChatVo();
        BeanUtils.copyProperties(chatPo, chatVo);

        String ownerId = chatVo.getOwnerId();
        String groupId = chatVo.getToId();

        ImGroupMessagePo groupMessageDto = imGroupMessageDubboService.last(ownerId, groupId);

        chatVo.setMessageTime(0L);
        if (Objects.nonNull(groupMessageDto)) {
            chatVo.setMessage(groupMessageDto.getMessageBody());
            chatVo.setMessageTime(groupMessageDto.getMessageTime());
        }

        Integer unread = imGroupMessageDubboService.selectReadStatus(groupId, ownerId, IMessageReadStatus.UNREAD.getCode());
        chatVo.setUnread(Objects.requireNonNullElse(unread, 0));

        ImGroupPo imGroupPo = imGroupDubboService.selectOne(groupId);
        if (imGroupPo != null) {
            chatVo.setName(imGroupPo.getGroupName());
            chatVo.setAvatar(imGroupPo.getAvatar());
            chatVo.setId(imGroupPo.getGroupId());
        }

        return chatVo;
    }

    /**
     * 构建ChatVo（create专用）
     */
    private ChatVo buildChatVo(ImChatPo imChatPO, Integer chatType) {
        ChatVo chatVo = new ChatVo();
        BeanUtils.copyProperties(imChatPO, chatVo);

        if (chatType.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
            ImUserDataPo imUserDataPo = imUserDataDubboService.selectOne(chatVo.getToId());
            if (imUserDataPo != null) {
                chatVo.setName(imUserDataPo.getName());
                chatVo.setAvatar(imUserDataPo.getAvatar());
                chatVo.setId(imUserDataPo.getUserId());
            }
        } else if (chatType.equals(IMessageType.GROUP_MESSAGE.getCode())) {
            ImGroupPo imGroupPo = imGroupDubboService.selectOne(chatVo.getToId());
            if (imGroupPo != null) {
                chatVo.setName(imGroupPo.getGroupName());
                chatVo.setAvatar(imGroupPo.getAvatar());
                chatVo.setId(imGroupPo.getGroupId());
            }
        }
        return chatVo;
    }

    /**
     * 设置单聊消息已读
     */
    private Result saveSingleMessageChatReadStatus(ChatDto chatDto) {
        long start = System.currentTimeMillis();
        try {
            ImSingleMessagePo updateMessage = new ImSingleMessagePo();
            updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
            updateMessage.setFromId(chatDto.getFromId());
            updateMessage.setToId(chatDto.getToId());

            boolean success = imSingleMessageDubboService.update(updateMessage);
            log.debug("单聊已读更新 from={} to={} 成功={} 耗时:{}ms", chatDto.getFromId(), chatDto.getToId(), success, System.currentTimeMillis() - start);
            return success ? Result.success("更新成功") : Result.failed("更新失败");
        } catch (Exception e) {
            log.error("单聊已读更新异常 from={} to={}", chatDto.getFromId(), chatDto.getToId(), e);
            throw new GlobalException(ResultCode.FAIL, "更新单聊已读失败");
        }
    }

    /**
     * 设置群聊消息已读
     */
    private Result saveGroupMessageChatReadStatus(ChatDto chatDto) {
        long start = System.currentTimeMillis();
        try {
            ImGroupMessageStatusPo updateMessage = new ImGroupMessageStatusPo();
            updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
            updateMessage.setGroupId(chatDto.getFromId());
            updateMessage.setToId(chatDto.getToId());

            boolean success = imGroupMessageDubboService.batchInsert(List.of(updateMessage));
            log.debug("群聊已读更新 groupId={} ownerId={} 成功={} 耗时:{}ms", chatDto.getToId(), chatDto.getFromId(), success, System.currentTimeMillis() - start);
            return success ? Result.success("更新成功") : Result.failed("更新失败");
        } catch (Exception e) {
            log.error("群聊已读更新异常 groupId={} ownerId={}", chatDto.getToId(), chatDto.getFromId(), e);
            throw new GlobalException(ResultCode.FAIL, "更新群聊已读失败");
        }
    }
}