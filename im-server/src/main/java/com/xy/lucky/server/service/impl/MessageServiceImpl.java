package com.xy.lucky.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMWebRTCType;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.mapper.MessageBeanMapper;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.dubbo.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.api.database.outbox.IMOutboxDubboService;
import com.xy.lucky.dubbo.api.id.ImIdDubboService;
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
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xy.lucky.core.constants.IMConstant.MQ_EXCHANGE_NAME;
import static com.xy.lucky.core.constants.IMConstant.USER_CACHE_PREFIX;

/**
 * 消息服务实现
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    private static final ObjectMapper jacksonMapper = new ObjectMapper();

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
     *
     * @param dto 消息 DTO
     * @return 发送结果
     */
    @Override
    public IMSingleMessage sendSingleMessage(IMSingleMessage dto) {
        log.info("Send single message: {}", dto);
        final String lockKey = LOCK_KEY_SEND_SINGLE + dto.getFromId() + ":" + dto.getToId();

        return withLock(lockKey, () -> {
            // 1. 生成 ID 与时间戳
            Long messageId = imIdDubboService.generateId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id).getLongId();
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            // 2. 填充 DTO 字段
            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            // 3. 转 PO 并设置标志
            ImSingleMessagePo messagePo = MessageBeanMapper.INSTANCE.toImSingleMessagePo(dto);
            messagePo.setDelFlag(IMStatus.YES.getCode());

            // 4. 异步入库/更新会话/Outbox（不阻塞发送路径）
            CompletableFuture.runAsync(() -> {
                createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "single.message." + dto.getToId(), messageTime);
                insertImSingleMessage(messagePo);
                createOrUpdateImChat(dto.getFromId(), dto.getToId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
                createOrUpdateImChat(dto.getToId(), dto.getFromId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
            }, asyncTaskExecutor);

            // 5. 检查接收者在线状态
            IMRegisterUser redisObj = redisUtil.get(USER_CACHE_PREFIX + dto.getToId());
            if (Objects.isNull(redisObj)) {
                log.info("sendSingleMessage: 目标不在线 from={} to={}", dto.getFromId(), dto.getToId());
                return dto;
            }

            // 6. 包装并发布到 broker
            IMessageWrap<Object> wrapper = new IMessageWrap<>().setCode(IMessageType.SINGLE_MESSAGE.getCode()).setData(dto).setIds(List.of(dto.getToId()));
            String payload = JacksonUtils.toJSONString(wrapper);
            publishToBroker(MQ_EXCHANGE_NAME, redisObj.getBrokerId(), payload, String.valueOf(messageId));

            log.info("sendSingleMessage: 发送成功 from={} to={} messageId={}", dto.getFromId(), dto.getToId(), dto.getMessageId());
            return dto;
        }, "发送私聊消息 " + dto.getMessageTempId() + " from=" + dto.getFromId() + " to=" + dto.getToId());
    }

    /**
     * 发送群聊消息
     *
     * @param dto 群聊消息 DTO
     * @return 发送结果
     */
    @Override
    public IMGroupMessage sendGroupMessage(IMGroupMessage dto) {
        log.info("Send group message: {}", dto);
        long startTime = System.currentTimeMillis();
        final String lockKey = LOCK_KEY_SEND_GROUP + dto.getGroupId() + ":" + dto.getFromId() + ":" + dto.getMessageTempId();

        return withLock(lockKey, () -> {
            Long messageId = imIdDubboService.generateId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id).getLongId();
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setSequence(messageTime);

            List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(dto.getGroupId());
            if (CollectionUtils.isEmpty(members)) {
                log.warn("sendGroupMessage: 群没有成员 groupId={}", dto.getGroupId());
                throw new MessageException("群聊成员为空，无法发送消息");
            }

            // 过滤掉发送者并构建 redis key 列表
            List<String> toRedisKeys = members.stream()
                    .filter(m -> !m.getMemberId().equals(dto.getFromId()))
                    .map(m -> USER_CACHE_PREFIX + m.getMemberId())
                    .collect(Collectors.toList());

            // 异步写入 outbox & message & status & 更新会话
            CompletableFuture.runAsync(() -> {
                createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "group.message." + dto.getGroupId(), messageTime);
                insertImGroupMessage(dto);
                setGroupReadStatus(String.valueOf(messageId), dto.getGroupId(), members);
                updateGroupChats(dto.getGroupId(), messageTime, members);
            }, asyncTaskExecutor);

            // 批量读取 redis 注册信息，按 broker 分组并发送
            List<Object> userObjs = redisUtil.batchGet(toRedisKeys);
            Map<String, List<String>> brokerMap = groupUsersByBroker(userObjs);

            brokerMap.forEach((brokerId, ids) -> {
                dto.setToList(ids);
                IMessageWrap<Object> wrapper = new IMessageWrap<>().setCode(IMessageType.GROUP_MESSAGE.getCode()).setData(dto).setIds(ids);
                publishToBroker(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapper), String.valueOf(messageId));
            });

            log.info("sendGroupMessage: 发送完成 groupId={} from={} messageId={} 耗时={}ms", dto.getGroupId(), dto.getFromId(), dto.getMessageId(), System.currentTimeMillis() - startTime);
            return dto;
        }, "发送群消息 groupId=" + dto.getGroupId() + " from=" + dto.getFromId());

    }

    /**
     * 发送视频消息
     *
     * @param videoMessage 视频消息
     */
    @Override
    public void sendVideoMessage(IMVideoMessage videoMessage) {
        long startTime = System.currentTimeMillis();
        final String lockKey = LOCK_KEY_SEND_VIDEO + videoMessage.getFromId() + ":" + videoMessage.getToId();

        withLock(lockKey, () -> {
            // 参数校验
            if (Objects.isNull(videoMessage.getFromId()) || Objects.isNull(videoMessage.getToId())) {
                throw new MessageException("参数无效，消息发送方或接收方为空");
            }

            // 检查接收者在线状态
            Object redisObj = redisUtil.get(USER_CACHE_PREFIX + videoMessage.getToId());
            if (Objects.isNull(redisObj)) {
                log.info("sendVideoMessage: 目标未登录 to={}", videoMessage.getToId());
                throw new MessageException("用户未登录");
            }

            // 获取登录用户信息
            IMRegisterUser targetUser = JacksonUtils.parseObject(redisObj, IMRegisterUser.class);
            if (targetUser == null || !StringUtils.hasText(targetUser.getBrokerId())) {
                throw new MessageException("用户未登录");
            }
            String brokerId = targetUser.getBrokerId();

            // 获取视频消息类型
            IMWebRTCType webRTCType = IMWebRTCType.getByCode(videoMessage.getType());
            if (webRTCType == null) {
                log.warn("sendVideoMessage: 未知视频消息类型 code={}", videoMessage.getType());
                throw new MessageException("无效消息类型");
            }

            // 构建消息
            IMessageWrap<Object> wrapMsg = new IMessageWrap<>()
                    .setCode(IMessageType.VIDEO_MESSAGE.getCode())
                    .setData(videoMessage)
                    .setIds(List.of(videoMessage.getToId()));


            MessagePostProcessor mpp = msg -> {
                msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return msg;
            };
            rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapMsg), mpp);
            log.info("sendVideoMessage: 发送成功 from={} to={} broker={}  总耗时 {}ms", videoMessage.getFromId(), videoMessage.getToId(), brokerId, System.currentTimeMillis() - startTime);
            return null;
        }, "发送视频消息 from=" + videoMessage.getFromId() + " to=" + videoMessage.getToId());
    }

    /**
     * 撤回消息
     *
     * @param dto 撤回消息参数
     */
    @Override
    public void recallMessage(IMessageAction dto) {
        log.info("Recall message: {}", dto);

        long startTime = System.currentTimeMillis();

        if (dto == null || dto.getMessageId() == null || dto.getOperatorId() == null) {
            throw new MessageException("参数无效");
        }

        //  参数
        final String messageId = dto.getMessageId();
        final String operatorId = dto.getOperatorId();
        final Long recallTime = dto.getRecallTime() != null ? dto.getRecallTime() : DateTimeUtils.getCurrentUTCTimestamp();
        final Integer messageType = dto.getMessageType();

        // 消息类型
        if (Objects.isNull(messageType)) {
            throw new MessageException("无法确定消息类型");
        }

        final String lockKey = LOCK_KEY_RECALL_MESSAGE + messageId;

        withLock(lockKey, () -> {

            // 构建消息
            Map<String, Object> recallPayload = new HashMap<>();
            recallPayload.put("_recalled", true);
            recallPayload.put("operatorId", operatorId);
            recallPayload.put("recallTime", recallTime);
            recallPayload.put("reason", dto.getReason());

            // 单聊撤回
            if (messageType.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
                ImSingleMessagePo msg = imSingleMessageDubboService.selectOne(messageId);
                if (Objects.isNull(msg)) throw new MessageException("消息不存在");
                if (!operatorId.equals(msg.getFromId())) throw new MessageException("无权撤回");

                Map<String, Object> body = safeParseMessageBody(msg.getMessageBody());
                if (Boolean.TRUE.equals(body.get("_recalled"))) throw new MessageException("消息已撤回");

                recallPayload.put("messageBody", msg.getMessageBody());
                ImSingleMessagePo update = new ImSingleMessagePo().setMessageId(messageId).setMessageBody(JacksonUtils.toJSONString(recallPayload)).setUpdateTime(recallTime);
                imSingleMessageDubboService.update(update);

                broadcastRecall(dto, messageId, recallTime, List.of(msg.getFromId(), msg.getToId()), IMessageType.SINGLE_MESSAGE.getCode());
            }

            // 群聊撤回
            if (messageType.equals(IMessageType.GROUP_MESSAGE.getCode())) {
                ImGroupMessagePo msg = imGroupMessageDubboService.selectOne(messageId);
                if (Objects.isNull(msg)) throw new MessageException("消息不存在");
                if (!operatorId.equals(msg.getFromId())) throw new MessageException("无权撤回");

                Map<String, Object> body = safeParseMessageBody(msg.getMessageBody());
                if (Boolean.TRUE.equals(body.get("_recalled"))) throw new MessageException("消息已撤回");

                recallPayload.put("groupId", msg.getGroupId());
                ImGroupMessagePo update = new ImGroupMessagePo().setMessageId(messageId).setMessageBody(JacksonUtils.toJSONString(recallPayload)).setUpdateTime(recallTime);
                imGroupMessageDubboService.update(update);

                List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(msg.getGroupId());
                if (!CollectionUtils.isEmpty(members)) {
                    List<String> memberIds = members.stream().map(ImGroupMemberPo::getMemberId).collect(Collectors.toList());
                    broadcastRecall(dto, messageId, recallTime, memberIds, IMessageType.GROUP_MESSAGE.getCode());
                }
            }
            log.info("recallMessage: 完成 messageId={} 耗时={}ms", messageId, System.currentTimeMillis() - startTime);
            return null;
        }, "撤回消息 messageId=" + messageId);
    }

    /**
     * 广播撤回通知（按 broker 聚合并发送）
     */
    private void broadcastRecall(IMessageAction dto, String messageId, Long recallTime, List<String> recipientIds, Integer messageType) {
        dto.setMessageTime(recallTime).setMessageId(messageId);

        List<String> redisKeys = recipientIds.stream().map(id -> USER_CACHE_PREFIX + id).collect(Collectors.toList());
        List<Object> redisObjs = redisUtil.batchGet(redisKeys);

        Map<String, List<String>> brokerMap = groupUsersByBroker(redisObjs);

        for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
            IMessageWrap<Object> wrap = new IMessageWrap<>().setCode(messageType).setData(dto).setIds(entry.getValue());
            publishToBroker(MQ_EXCHANGE_NAME, entry.getKey(), JacksonUtils.toJSONString(wrap), messageId);
        }
        log.info("broadcastRecall: 完成 messageId={} messageType={} recipients={}", messageId, messageType, recipientIds.size());
    }

    @Override
    public Map<Integer, Object> list(ChatDto chatDto) {
        String userId = chatDto.getFromId();
        Long sequence = chatDto.getSequence();

        Map<Integer, Object> map = new HashMap<>();

        // 获取单聊消息列表
        List<ImSingleMessagePo> singleMessages = imSingleMessageDubboService.selectList(userId, sequence);
        if (!CollectionUtils.isEmpty(singleMessages)) {
            map.put(IMessageType.SINGLE_MESSAGE.getCode(), singleMessages);
        }

        // 获取群聊消息列表
        List<ImGroupMessagePo> groupMessages = imGroupMessageDubboService.selectList(userId, sequence);
        if (!CollectionUtils.isEmpty(groupMessages)) {
            map.put(IMessageType.GROUP_MESSAGE.getCode(), groupMessages);
        }

        return map;
    }

    /**
     * 保存单聊消息
     */
    private void insertImSingleMessage(ImSingleMessagePo messagePo) {
        try {
            if (!imSingleMessageDubboService.insert(messagePo)) {
                log.error("insertImSingleMessage: 保存私聊消息失败 messageId={}", messagePo.getMessageId());
            }
        } catch (Exception e) {
            log.error("insertImSingleMessage 异常 messageId={}", messagePo.getMessageId(), e);
        }
    }

    /**
     * 保存群聊消息
     */
    private void insertImGroupMessage(IMGroupMessage dto) {
        try {
            ImGroupMessagePo po = MessageBeanMapper.INSTANCE.toImGroupMessagePo(dto);
            po.setDelFlag(IMStatus.YES.getCode());
            if (!imGroupMessageDubboService.insert(po)) {
                log.error("insertImGroupMessage: 保存群消息失败 messageId={}", dto.getMessageId());
            }
        } catch (Exception e) {
            log.error("insertImGroupMessage 异常 messageId={}", dto.getMessageId(), e);
        }
    }

    /**
     * 创建或更新会话
     */
    private void createOrUpdateImChat(String ownerId, String toId, Long messageTime, Integer chatType) {
        try {
            ImChatPo chatPo = imChatDubboService.selectOne(ownerId, toId, chatType);
            if (Objects.isNull(chatPo)) {
                chatPo = new ImChatPo()
                        .setChatId(imIdDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.chat_id).getStringId())
                        .setOwnerId(ownerId)
                        .setToId(toId)
                        .setSequence(messageTime)
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setDelFlag(IMStatus.YES.getCode())
                        .setChatType(chatType);
                if (!imChatDubboService.insert(chatPo)) {
                    log.error("createOrUpdateImChat: 保存会话失败 ownerId={} toId={}", ownerId, toId);
                }
            } else {
                chatPo.setSequence(messageTime);
                if (!imChatDubboService.update(chatPo)) {
                    log.error("createOrUpdateImChat: 更新会话失败 ownerId={} toId={}", ownerId, toId);
                }
            }
        } catch (Exception e) {
            log.error("createOrUpdateImChat 异常 ownerId={} toId={}", ownerId, toId, e);
        }
    }

    /**
     * 创建或更新群聊会话
     */
    private void updateGroupChats(String groupId, Long messageTime, List<ImGroupMemberPo> members) {
        for (ImGroupMemberPo member : members) {
            createOrUpdateImChat(member.getMemberId(), groupId, messageTime, IMessageType.GROUP_MESSAGE.getCode());
        }
    }

    /**
     * 设置群聊已读
     */
    private void setGroupReadStatus(String messageId, String groupId, List<ImGroupMemberPo> members) {
        try {
            List<ImGroupMessageStatusPo> statusList = members.stream()
                    .map(m -> new ImGroupMessageStatusPo().setMessageId(messageId).setGroupId(groupId)
                            .setReadStatus(IMessageReadStatus.UNREAD.getCode()).setToId(m.getMemberId()))
                    .collect(Collectors.toList());
            imGroupMessageDubboService.batchInsert(statusList);
        } catch (Exception e) {
            log.error("setGroupReadStatus 异常 messageId={}", messageId, e);
        }
    }


    /**
     * 创建 Outbox
     */
    private void createOutbox(String messageId, String payload, String exchange, String routingKey, Long messageTime) {
        long outboxId = System.nanoTime();
        IMOutboxPo po = new IMOutboxPo()
                .setId(outboxId)
                .setMessageId(messageId)
                .setPayload(payload)
                .setExchange(exchange)
                .setRoutingKey(routingKey)
                .setAttempts(0)
                .setStatus("PENDING")
                .setCreatedAt(messageTime)
                .setUpdatedAt(messageTime);

        try {
            if (!imOutboxDubboService.saveOrUpdate(po)) {
                log.warn("createOutbox: Outbox 保存失败 messageId={}", messageId);
            } else {
                messageToOutboxIdMap.put(messageId, outboxId);
            }
        } catch (Exception e) {
            log.error("createOutbox 异常 messageId={}", messageId, e);
        }
    }

    /**
     * 发布到 Broker（异步执行），并在失败时标记 outbox
     */
    private void publishToBroker(String exchange, String routingKey, String payload, String messageId) {
        CompletableFuture.runAsync(() -> {
            try {
                MessagePostProcessor mpp = msg -> {
                    msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    msg.getMessageProperties().setCorrelationId(messageId);
                    return msg;
                };
                CorrelationData corr = new CorrelationData(messageId);
                rabbitTemplate.convertAndSend(exchange, routingKey, payload, mpp, corr);
            } catch (Exception e) {
                log.error("publishToBroker: 发布失败 messageId={}", messageId, e);
                Long outboxId = messageToOutboxIdMap.get(messageId);
                if (outboxId != null) {
                    try {
                        imOutboxDubboService.markAsFailed(outboxId, e.getMessage(), 1);
                    } catch (Exception ex) {
                        log.error("publishToBroker: 标记 Outbox 失败 outboxId={}", outboxId, ex);
                    }
                }
            }
        }, asyncTaskExecutor);
    }

    /**
     * 处理 broker 确认回调（ACK/NACK/returned）
     */
    private void handleConfirm(String messageId, boolean ack, String cause) {
        if (messageId == null) return;
        Long outboxId = messageToOutboxIdMap.get(messageId);
        try {
            if (ack) {
                imOutboxDubboService.updateStatus(outboxId, "SENT", 1);
                messageToOutboxIdMap.remove(messageId);
            } else {
                imOutboxDubboService.updateStatus(outboxId, "PENDING", 0);
            }
        } catch (Exception e) {
            log.error("handleConfirm: 更新 Outbox 失败 outboxId={} messageId={}", outboxId, messageId, e);
        }
    }

    /**
     * 重试 Pending 消息（防并发，使用 Redisson 锁）
     */
    public void retryPendingMessages() {
        long startTime = System.currentTimeMillis();
        RLock lock = redissonClient.getLock(LOCK_KEY_RETRY_PENDING);
        if (!tryLockSimple(lock, LOCK_WAIT_TIME, LOCK_LEASE_TIME)) {
            log.warn("retryPendingMessages: 无法获取锁，跳过本次重试");
            return;
        }
        try {
            List<IMOutboxPo> pending = imOutboxDubboService.listByStatus("PENDING", 100);
            if (CollectionUtils.isEmpty(pending)) {
                return;
            }
            for (IMOutboxPo o : pending) {
                int attempts = o.getAttempts() + 1;
                try {
                    MessagePostProcessor mpp = msg -> {
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        msg.getMessageProperties().setCorrelationId(o.getMessageId());
                        return msg;
                    };
                    CorrelationData corr = new CorrelationData(o.getMessageId());
                    rabbitTemplate.convertAndSend(o.getExchange(), o.getRoutingKey(), o.getPayload(), mpp, corr);
                    imOutboxDubboService.updateStatus(o.getId(), "SENT", attempts);
                } catch (Exception e) {
                    log.error("retryPendingMessages: 重试失败 messageId={}", o.getMessageId(), e);
                    imOutboxDubboService.markAsFailed(o.getId(), e.getMessage(), attempts);
                }
            }
        } catch (Exception e) {
            log.error("retryPendingMessages: 异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("retryPendingMessages: 总耗时 {}ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 将 redis batchGet 返回的对象解析并按 brokerId 分组
     */
    private Map<String, List<String>> groupUsersByBroker(List<Object> redisObjs) {
        Map<String, List<String>> brokerMap = new HashMap<>();
        if (CollectionUtils.isEmpty(redisObjs)) return brokerMap;
        for (Object obj : redisObjs) {
            if (Objects.nonNull(obj)) {
                IMRegisterUser user = JacksonUtils.parseObject(obj, IMRegisterUser.class);
                if (user != null && user.getBrokerId() != null) {
                    brokerMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
                }
            }
        }
        return brokerMap;
    }

    /**
     * 安全解析消息体：支持 Map / String(JSON) / 其它对象
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeParseMessageBody(Object raw) {
        if (raw == null) return Collections.emptyMap();
        if (raw instanceof Map) return (Map<String, Object>) raw;
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            try {
                return jacksonMapper.readValue(s, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                return Map.of("raw", s);
            }
        }
        try {
            return jacksonMapper.convertValue(raw, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of("raw", String.valueOf(raw));
        }
    }

    /**
     * 内部通用：在 Redisson 锁保护下执行任务（锁逻辑保留在类内）
     *
     * @param lockKey    分布式锁 key
     * @param task       要执行的任务（Callable），可返回结果或 null
     * @param actionDesc 操作描述（用于日志）
     * @param <T>        返回类型
     * @return task 返回值
     */
    private <T> T withLock(String lockKey, java.util.concurrent.Callable<T> task, String actionDesc) {
        RLock lock = redissonClient.getLock(lockKey);
        long t0 = System.currentTimeMillis();
        try {
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("{} - 获取锁失败 key={}", actionDesc, lockKey);
                throw new MessageException("操作中，请稍后重试");
            }
            return task.call();
        } catch (MessageException me) {
            // 业务异常直接抛出
            throw me;
        } catch (Exception e) {
            log.error("{} - 执行异常 key={} 耗时={}ms", actionDesc, lockKey, System.currentTimeMillis() - t0, e);
            throw new MessageException("操作失败");
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception ex) {
                log.warn("释放锁异常 key={} error={}", lockKey, ex.getMessage());
            }
            log.debug("{} - 总耗时 {} ms", actionDesc, System.currentTimeMillis() - t0);
        }
    }

    /**
     * 简单尝试获取锁，返回 boolean（不抛异常）
     */
    private boolean tryLockSimple(RLock lock, long waitSeconds, long leaseSeconds) {
        try {
            return lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("tryLockSimple 被中断", e);
            return false;
        } catch (Exception e) {
            log.error("tryLockSimple 异常", e);
            return false;
        }
    }
}
