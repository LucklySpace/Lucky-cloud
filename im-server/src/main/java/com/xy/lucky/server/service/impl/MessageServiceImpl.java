package com.xy.lucky.server.service.impl;

import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.mapper.MessageBeanMapper;
import com.xy.lucky.domain.po.IMOutboxPo;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import com.xy.lucky.dubbo.web.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.outbox.IMOutboxDubboService;
import com.xy.lucky.dubbo.web.api.id.ImIdDubboService;
import com.xy.lucky.mq.rabbit.core.RabbitTemplateFactory;
import com.xy.lucky.server.config.IdGeneratorConstant;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public Mono<IMSingleMessage> sendSingleMessage(IMSingleMessage dto) {
        return Mono.fromCallable(() -> {
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
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 发送群聊消息
     */
    @Override
    public Mono<IMGroupMessage> sendGroupMessage(IMGroupMessage dto) {
        return Mono.fromCallable(() -> {
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

                        asyncTaskExecutor.execute(() -> {
                            try {
                                createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "group.message." + dto.getGroupId(), messageTime);
                                insertImGroupMessage(messagePo);
                            } catch (Exception e) {
                                log.error("Async DB tasks failed for group messageId: {}", messageId, e);
                            }
                        });

                        List<ImGroupMemberPo> userIds = imGroupMemberDubboService.queryList(dto.getGroupId());
                        if (CollectionUtils.isEmpty(userIds)) {
                            log.warn("群聊没有任何成员 groupId={}", dto.getGroupId());
                            return dto;
                        }

                        userIds.remove(dto.getFromId());
                        if (CollectionUtils.isEmpty(userIds)) {
                            return dto;
                        }

                        List<String> redisKeys = userIds.stream().map(id -> USER_CACHE_PREFIX + id).collect(Collectors.toList());
                        List<Object> redisObjs = redisUtil.batchGet(redisKeys);
                        if (CollectionUtils.isEmpty(redisObjs)) {
                            return dto;
                        }

                        Map<String, List<String>> brokerUserMap = new HashMap<>();
                        for (Object obj : redisObjs) {
                            if (obj instanceof IMRegisterUser user && user.getBrokerId() != null && user.getUserId() != null) {
                                brokerUserMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
                            }
                        }

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
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> sendVideoMessage(IMVideoMessage videoMessage) {
        return Mono.fromCallable(() -> {
                    log.info("Send video message: {}", videoMessage);
                    String lockKey = LOCK_KEY_SEND_VIDEO + videoMessage.getFromId() + ":" + videoMessage.getToId();
                    return withLockSync(lockKey, "发送视频消息", () -> {
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
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> recallMessage(IMessageAction dto) {
        return Mono.fromCallable(() -> {
                    log.info("Recall message: {}", dto);
                    final String lockKey = LOCK_KEY_RECALL_MESSAGE + dto.getMessageId();
                    return withLockSync(lockKey, "撤回消息 " + dto.getMessageId(), () -> {
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
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void processRecallSingleSync(ImSingleMessagePo msg, IMessageAction dto) {
        if (!msg.getFromId().equals(dto.getOperatorId())) {
            throw new MessageException("无权撤回他人消息");
        }
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        if (now - msg.getMessageTime() > 2 * 60 * 1000) {
            throw new MessageException("发送时间超过2分钟，无法撤回");
        }

        msg.setMessageContentType(IMessageType.RECALL_MESSAGE.getCode());
        msg.setMessageBody("撤回了一条消息");
        imSingleMessageDubboService.modify(msg);

        IMRegisterUser receiver = (IMRegisterUser) redisUtil.get(USER_CACHE_PREFIX + msg.getToId());
        if (receiver == null || receiver.getBrokerId() == null) return;
        IMessageWrap<Object> wrapper = new IMessageWrap<>()
                .setCode(IMessageType.RECALL_MESSAGE.getCode())
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

        msg.setMessageContentType(IMessageType.RECALL_MESSAGE.getCode());
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
                    .setCode(IMessageType.RECALL_MESSAGE.getCode())
                    .setData(dto)
                    .setIds(users);
            publishToBrokerSync(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapper), null);
        });
    }

    @Override
    public Mono<Map<Integer, Object>> list(ChatDto chatDto) {
        return Mono.fromCallable(() -> {

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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private <T> T withLockSync(String key, String logDesc, ThrowingSupplier<T> action) throws Exception {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new MessageException("无法获取锁: " + logDesc);
            }
            return action.get();
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

    private void insertImSingleMessage(ImSingleMessagePo po) {
        imSingleMessageDubboService.creat(po); // Blocking
    }

    private void insertImGroupMessage(ImGroupMessagePo po) {
        imGroupMessageDubboService.creat(po); // Blocking
    }

    private void createOrUpdateImChat(String userId, String friendId, Long time, Integer type) {
        // imChatDubboService.createOrUpdate(userId, friendId, time, type);
    }

    private void handleConfirm(String messageId, boolean success, String error) {
        if (messageId == null) return;
        Long outboxId = messageToOutboxIdMap.remove(messageId);
        if (outboxId != null) {
            // Update outbox status asynchronously
            Mono.fromRunnable(() -> {
                try {
                    imOutboxDubboService.modifyStatus(outboxId, String.valueOf(success ? 1 : 2), 1); // 1: SENT, 2: FAILED
                } catch (Exception e) {
                    log.error("Failed to update outbox status", e);
                }
            }).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
