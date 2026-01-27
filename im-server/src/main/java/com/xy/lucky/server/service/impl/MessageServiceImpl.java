package com.xy.lucky.server.service.impl;

import com.xy.lucky.core.enums.IMActionType;
import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.dubbo.web.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.outbox.IMOutboxDubboService;
import com.xy.lucky.dubbo.web.api.id.ImIdDubboService;
import com.xy.lucky.mq.rabbit.core.RabbitTemplateFactory;
import com.xy.lucky.server.config.IdGeneratorConstant;
import com.xy.lucky.server.domain.dto.ChatDto;
import com.xy.lucky.server.domain.mapper.MessageBeanMapper;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.MessageService;
import com.xy.lucky.server.utils.RedisUtil;
import com.xy.lucky.utils.json.JacksonUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xy.lucky.core.constants.IMConstant.MQ_EXCHANGE_NAME;
import static com.xy.lucky.core.constants.IMConstant.USER_CACHE_PREFIX;

/**
 * 消息服务实现 (WebFlux version)
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    /* 分布式锁 Key 常量 */
    private static final String LOCK_KEY_SEND_SINGLE = "lock:send:single:";
    private static final String LOCK_KEY_SEND_GROUP = "lock:send:group:";
    private static final String LOCK_KEY_SEND_VIDEO = "lock:send:video:";
    private static final String LOCK_KEY_RECALL_MESSAGE = "recall:message:lock:";
    private static final String LOCK_KEY_RETRY_PENDING = "lock:retry:pending";

    private static final long LOCK_WAIT_TIME = 5L;   // 锁等待（秒）
    private static final long LOCK_LEASE_TIME = 10L; // 锁租期（秒）

    // 顶层映射：messageId -> outboxId（内存缓存，便于回调处理）
    private final Map<String, Long> messageToOutboxIdMap = new ConcurrentHashMap<>();

    /* ========== Dubbo 服务引用 ========== */
    @DubboReference
    private ImChatDubboService imChatDubboService;
    @DubboReference
    private ImGroupMemberDubboService imGroupMemberDubboService;
    @DubboReference
    private ImSingleMessageDubboService imSingleMessageDubboService;
    @DubboReference
    private ImGroupMessageDubboService imGroupMessageDubboService;
    @DubboReference
    private IMOutboxDubboService imOutboxDubboService;
    @DubboReference
    private ImIdDubboService imIdDubboService;

    /* ========== 其他资源 ========== */
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;

    private RabbitTemplate rabbitTemplate;

    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    /**
     * 初始化 RabbitMQ
     */
    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate(
                (correlationData, ack, cause) -> {
                    String messageId = correlationData != null ? correlationData.getId() : null;
                    if (ack) {
                        log.info("Confirm: message {} ACKed by broker", messageId);
                        handleConfirm(messageId, true, null);
                    } else {
                        log.warn("Confirm: message {} NACKed by broker, cause={}", messageId, cause);
                        handleConfirm(messageId, false, cause != null ? cause : "broker_nack");
                    }
                },
                returnedMessage -> {
                    try {
                        log.warn("ReturnCallback: exchange={}, routingKey={}, replyText={}",
                                returnedMessage.getExchange(),
                                returnedMessage.getRoutingKey(),
                                returnedMessage.getReplyText());
                        Object corr = returnedMessage.getMessage().getMessageProperties().getCorrelationId();
                        String messageId = corr != null ? corr.toString() : null;
                        handleConfirm(messageId, false, "message_returned:" + returnedMessage.getReplyText());
                    } catch (Exception ex) {
                        log.error("Process message exception: {}", ex.getMessage(), ex);
                    }
                }
        );
    }

    /**
     * 发送单聊消息
     */
    @Override
    public IMSingleMessage sendSingleMessage(IMSingleMessage dto) {
        log.info("Send single message: {}", dto);
        final String lockKey = LOCK_KEY_SEND_SINGLE + dto.getFromId() + ":" + dto.getToId();
        return withLockSync(lockKey, "发送私聊消息 " + dto.getMessageTempId(), () -> {
            Long messageId = imIdDubboService.generateId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id).getLongId();
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            ImSingleMessagePo messagePo = MessageBeanMapper.INSTANCE.toImSingleMessagePo(dto);
            messagePo.setDelFlag(IMStatus.YES.getCode());

            asyncTaskExecutor.execute(() -> {
                try {
                    createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "single.message." + dto.getToId(), messageTime);
                    insertImSingleMessage(messagePo);
                    createOrUpdateImChat(dto.getFromId(), dto.getToId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
                    createOrUpdateImChat(dto.getToId(), dto.getFromId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
                } catch (Exception e) {
                    log.error("Async DB tasks failed for messageId: {}", messageId, e);
                }
            });

            IMRegisterUser receiver = (IMRegisterUser) redisUtil.get(USER_CACHE_PREFIX + dto.getToId());
            if (receiver == null || receiver.getUserId() == null) {
                log.info("sendSingleMessage: 目标不在线 from={} to={}", dto.getFromId(), dto.getToId());
                return dto;
            }

            IMessageWrap<Object> wrapper = new IMessageWrap<>()
                    .setCode(IMessageType.SINGLE_MESSAGE.getCode())
                    .setData(dto)
                    .setIds(List.of(dto.getToId()));
            String payload = JacksonUtils.toJSONString(wrapper);
            publishToBrokerSync(MQ_EXCHANGE_NAME, receiver.getBrokerId(), payload, String.valueOf(messageId));

            log.info("sendSingleMessage: 发送成功 from={} to={} messageId={}", dto.getFromId(), dto.getToId(), dto.getMessageId());
            return dto;
        });
    }

    /**
     * 发送群聊消息
     */
    @Override
    public IMGroupMessage sendGroupMessage(IMGroupMessage dto) {
        log.info("Send group message: {}", dto);
        final String lockKey = LOCK_KEY_SEND_GROUP + dto.getGroupId() + ":" + dto.getFromId() + ":" + dto.getMessageTempId();
        return withLockSync(lockKey, "发送群聊消息 " + dto.getMessageTempId(), () -> {
            Long messageId = imIdDubboService.generateId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id).getLongId();
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            ImGroupMessagePo messagePo = MessageBeanMapper.INSTANCE.toImGroupMessagePo(dto);
            messagePo.setDelFlag(IMStatus.YES.getCode());

            List<ImGroupMemberPo> members = imGroupMemberDubboService.queryList(dto.getGroupId());
            if (CollectionUtils.isEmpty(members)) {
                log.warn("群聊没有任何成员 groupId={}", dto.getGroupId());
                return dto;
            }

            List<String> ids = members.stream().map(member -> USER_CACHE_PREFIX + member.getMemberId()).toList();

            asyncTaskExecutor.execute(() -> {
                try {
                    createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "group.message." + dto.getGroupId(), messageTime);
                    insertImGroupMessage(messagePo);
                    setGroupReadStatus(String.valueOf(messageId), dto.getGroupId(), members);
                    updateGroupChats(dto.getGroupId(), messageTime, members);
                } catch (Exception e) {
                    log.error("Async DB tasks failed for group messageId: {}", messageId, e);
                }
            });

            List<Object> userObjs = redisUtil.batchGet(ids);
            if (CollectionUtils.isEmpty(userObjs)) {
                return dto;
            }

            Map<String, List<String>> brokerUserMap = new HashMap<>();
            userObjs.stream().filter(Objects::nonNull).forEach(obj -> {
                IMRegisterUser user = JacksonUtils.parseObject(obj, IMRegisterUser.class);
                if (user == null || user.getUserId() == null) {
                    log.info("sendGroupMessage: 群成员不在线 groupId={} userId={}", dto.getGroupId(), user != null ? user.getUserId() : null);
                    return;
                }
                if (user.getUserId().equals(dto.getFromId())) {
                    return;
                }
                brokerUserMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
            });

            brokerUserMap.forEach((brokerId, users) -> {
                IMessageWrap<Object> wrapper = new IMessageWrap<>()
                        .setCode(IMessageType.GROUP_MESSAGE.getCode())
                        .setData(dto)
                        .setIds(users);
                publishToBrokerSync(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapper), String.valueOf(messageId));
            });

            log.info("sendGroupMessage: 发送成功 from={} groupId={} messageId={}", dto.getFromId(), dto.getGroupId(), dto.getMessageId());
            return dto;
        });
    }

    @Override
    public void sendVideoMessage(IMVideoMessage videoMessage) {
        log.info("Send video message: {}", videoMessage);
        String lockKey = LOCK_KEY_SEND_VIDEO + videoMessage.getFromId() + ":" + videoMessage.getToId();
        withLockSync(lockKey, "发送视频消息", () -> {
            IMRegisterUser receiver = (IMRegisterUser) redisUtil.get(USER_CACHE_PREFIX + videoMessage.getToId());
            if (receiver == null || receiver.getUserId() == null) {
                log.info("sendVideoMessage: 目标不在线 from={} to={}", videoMessage.getFromId(), videoMessage.getToId());
                return null;
            }
            IMessageWrap<Object> wrapper = new IMessageWrap<>()
                    .setCode(IMessageType.VIDEO_MESSAGE.getCode())
                    .setData(videoMessage)
                    .setIds(List.of(videoMessage.getToId()));
            publishToBrokerSync(MQ_EXCHANGE_NAME, receiver.getBrokerId(), JacksonUtils.toJSONString(wrapper), null);
            return null;
        });
    }

    @Override
    public void recallMessage(IMessageAction dto) {
        log.info("Recall message: {}", dto);
        final String lockKey = LOCK_KEY_RECALL_MESSAGE + dto.getMessageId();
        withLockSync(lockKey, "撤回消息 " + dto.getMessageId(), () -> {
            ImSingleMessagePo singleMsg = imSingleMessageDubboService.queryOne(dto.getMessageId());
            if (singleMsg != null) {
                processRecallSingleSync(singleMsg, dto);
                return null;
            }

            ImGroupMessagePo groupMsg = imGroupMessageDubboService.queryOne(dto.getMessageId());
            if (groupMsg != null) {
                processRecallGroupSync(groupMsg, dto);
                return null;
            }
            throw new MessageException("消息不存在");
        });
    }

    private void processRecallSingleSync(ImSingleMessagePo msg, IMessageAction dto) {
        if (!msg.getFromId().equals(dto.getOperatorId())) {
            throw new MessageException("无权撤回他人消息");
        }
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        if (now - msg.getMessageTime() > 2 * 60 * 1000) {
            throw new MessageException("发送时间超过2分钟，无法撤回");
        }

        msg.setMessageContentType(IMActionType.RECALL_MESSAGE.getCode());
        msg.setMessageBody("撤回了一条消息");
        imSingleMessageDubboService.modify(msg);

        IMRegisterUser receiver = (IMRegisterUser) redisUtil.get(USER_CACHE_PREFIX + msg.getToId());
        if (receiver == null || receiver.getBrokerId() == null) return;
        IMessageWrap<Object> wrapper = new IMessageWrap<>()
                .setCode(IMActionType.RECALL_MESSAGE.getCode())
                .setData(dto)
                .setIds(List.of(msg.getToId()));
        publishToBrokerSync(MQ_EXCHANGE_NAME, receiver.getBrokerId(), JacksonUtils.toJSONString(wrapper), null);
    }

    private void processRecallGroupSync(ImGroupMessagePo msg, IMessageAction dto) {
        if (!msg.getFromId().equals(dto.getOperatorId())) {
            throw new MessageException("无权撤回他人消息");
        }
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        if (now - msg.getMessageTime() > 2 * 60 * 1000) {
            throw new MessageException("发送时间超过2分钟，无法撤回");
        }

        msg.setMessageContentType(IMActionType.RECALL_MESSAGE.getCode());
        msg.setMessageBody("撤回了一条消息");
        imGroupMessageDubboService.modify(msg);

        List<ImGroupMemberPo> memberIds = imGroupMemberDubboService.queryList(msg.getGroupId());
        if (CollectionUtils.isEmpty(memberIds)) return;

        List<String> redisKeys = memberIds.stream().map(id -> USER_CACHE_PREFIX + id).collect(Collectors.toList());
        List<Object> redisObjs = redisUtil.batchGet(redisKeys);
        if (CollectionUtils.isEmpty(redisObjs)) return;

        Map<String, List<String>> brokerUserMap = new HashMap<>();
        for (Object obj : redisObjs) {
            if (obj instanceof IMRegisterUser user && user.getBrokerId() != null && user.getUserId() != null) {
                brokerUserMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
            }
        }

        brokerUserMap.forEach((brokerId, users) -> {
            IMessageWrap<Object> wrapper = new IMessageWrap<>()
                    .setCode(IMActionType.RECALL_MESSAGE.getCode())
                    .setData(dto)
                    .setIds(users);
            publishToBrokerSync(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapper), null);
        });
    }

    @Override
    public Map<Integer, Object> list(ChatDto chatDto) {
        String userId = chatDto.getFromId();
        Long sequence = chatDto.getSequence();

        Map<Integer, Object> map = new HashMap<>();

        List<ImSingleMessagePo> singleMessages = imSingleMessageDubboService.queryList(userId, sequence);
        if (!CollectionUtils.isEmpty(singleMessages)) {
            map.put(IMessageType.SINGLE_MESSAGE.getCode(), singleMessages);
        }

        List<ImGroupMessagePo> groupMessages = imGroupMessageDubboService.queryList(userId, sequence);
        if (!CollectionUtils.isEmpty(groupMessages)) {
            map.put(IMessageType.GROUP_MESSAGE.getCode(), groupMessages);
        }

        return map;
    }

    private <T> T withLockSync(String key, String logDesc, ThrowingSupplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new MessageException("无法获取锁: " + logDesc);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageException("无法获取锁: " + logDesc);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void publishToBrokerSync(String exchange, String routingKey, String payload, String messageId) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload, new CorrelationData(messageId));
    }

    private void createOutbox(String messageId, String payload, String exchange, String routingKey, Long timestamp) {
        IMOutboxPo outboxPo = new IMOutboxPo();
        outboxPo.setMessageId(messageId);
        outboxPo.setPayload(payload);
        outboxPo.setExchange(exchange);
        outboxPo.setRoutingKey(routingKey);
        outboxPo.setStatus(String.valueOf(0)); // 0: PENDING

//        Long outboxId = imOutboxDubboService.insert(outboxPo).getId(); // Blocking
//        messageToOutboxIdMap.put(messageId, outboxId);
    }


    /**
     * 插入私聊消息
     */
    private void insertImSingleMessage(ImSingleMessagePo po) {
        try {
            if (!imSingleMessageDubboService.creat(po)) {
                log.error("保存私聊消息失败 messageId={}", po.getMessageId());
            }
        } catch (Exception e) {
            log.error("插入私聊消息异常 messageId={}", po.getMessageId(), e);
        }
    }

    /**
     * 插入群聊消息
     */
    private void insertImGroupMessage(ImGroupMessagePo po) {
        try {
            if (!imGroupMessageDubboService.creat(po)) {
                log.error("保存群消息失败 messageId={}", po.getMessageId());
            }
        } catch (Exception e) {
            log.error("插入群消息异常 messageId={}", po.getMessageId(), e);
        }
    }

    /**
     * 创建或更新会话（通用）
     */
    private void createOrUpdateImChat(String ownerId, String toId, Long messageTime, Integer chatType) {
        try {
            ImChatPo chatPo = imChatDubboService.queryOne(ownerId, toId, chatType);
            if (Objects.isNull(chatPo)) {
                chatPo = new ImChatPo()
                        .setChatId(imIdDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.chat_id).getStringId())
                        .setOwnerId(ownerId)
                        .setToId(toId)
                        .setSequence(messageTime)
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setChatType(chatType);
                if (!imChatDubboService.creat(chatPo)) {
                    log.error("保存会话失败 ownerId={} toId={}", ownerId, toId);
                }
            } else {
                chatPo.setSequence(messageTime);
                if (!imChatDubboService.modify(chatPo)) {
                    log.error("更新会话失败 ownerId={} toId={}", ownerId, toId);
                }
            }
        } catch (Exception e) {
            log.error("创建/更新会话异常 ownerId={} toId={}", ownerId, toId, e);
        }
    }

    /**
     * 更新群聊会话
     */
    private void updateGroupChats(String groupId, Long messageTime, List<ImGroupMemberPo> members) {
        for (ImGroupMemberPo member : members) {
            createOrUpdateImChat(member.getMemberId(), groupId, messageTime, IMessageType.GROUP_MESSAGE.getCode());
        }
    }

    /**
     * 设置群消息读状态
     */
    private void setGroupReadStatus(String messageId, String groupId, List<ImGroupMemberPo> members) {
        try {
            List<ImGroupMessageStatusPo> statusList = members.stream()
                    .map(m -> new ImGroupMessageStatusPo().setMessageId(messageId).setGroupId(groupId)
                            .setReadStatus(IMessageReadStatus.UNREAD.getCode()).setToId(m.getMemberId()))
                    .collect(Collectors.toList());
            imGroupMessageDubboService.creatBatch(statusList);
        } catch (Exception e) {
            log.error("设置群读状态失败 messageId={}", messageId, e);
        }
    }


    private void handleConfirm(String messageId, boolean success, String error) {
        if (messageId == null) return;
        Long outboxId = messageToOutboxIdMap.remove(messageId);
        if (outboxId != null) {
            // Update outbox status asynchronously
            asyncTaskExecutor.execute(() -> {
                try {
                    imOutboxDubboService.modifyStatus(outboxId, String.valueOf(success ? 1 : 2), 1); // 1: SENT, 2: FAILED
                } catch (Exception e) {
                    log.error("Failed to update outbox status", e);
                }
            });
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
