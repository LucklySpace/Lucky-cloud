package com.xy.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.core.enums.IMStatus;
import com.xy.core.enums.IMessageReadStatus;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.*;
import com.xy.domain.dto.ChatDto;
import com.xy.domain.po.*;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.api.database.chat.ImChatFeign;
import com.xy.server.api.database.group.ImGroupFeign;
import com.xy.server.api.database.message.ImMessageFeign;
import com.xy.server.api.database.outbox.IMOutboxFeign;
import com.xy.server.api.id.IdGeneratorConstant;
import com.xy.server.api.id.ImIdGeneratorFeign;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.service.MessageService;
import com.xy.server.utils.JsonUtil;
import com.xy.server.utils.RedisUtil;
import com.xy.utils.DateTimeUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.xy.core.constants.IMConstant.MQ_EXCHANGE_NAME;
import static com.xy.core.constants.IMConstant.USER_CACHE_PREFIX;

@Slf4j
@Service

public class MessageServiceImpl implements MessageService {

    private static final ObjectMapper jacksonMapper = new ObjectMapper();

    private final Map<String, Long> messageToOutboxId = new ConcurrentHashMap<>();

    private static final String LOCK_KEY_SEND_SINGLE = "lock:send:single:";
    private static final String LOCK_KEY_SEND_GROUP = "lock:send:group:";
    private static final String LOCK_KEY_SEND_VIDEO = "lock:send:video:";
    private static final String LOCK_KEY_RECALL_MESSAGE = "recall:message:lock:";
    private static final String LOCK_KEY_RETRY_PENDING = "lock:retry:pending";

    private static final long LOCK_WAIT_TIME = 5L; // 锁等待5s
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有10s

    @Resource
    private ImMessageFeign imMessageFeign;
    @Resource
    private ImChatFeign imChatFeign;
    @Resource
    private ImIdGeneratorFeign imIdGeneratorFeign;
    @Resource
    private IMOutboxFeign imOutboxFeign;
    @Resource
    private ImGroupFeign imGroupFeign;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;
    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;

    private RabbitTemplate rabbitTemplate;

    /**
     * 初始化 RabbitTemplate 并绑定确认回调与返回回调
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
                        log.warn("Confirm: message {} NACKed by broker, cause: {}", messageId, cause);
                        handleConfirm(messageId, false, cause != null ? cause : "broker_nack");
                    }
                },
                returnedMessage -> {
                    try {
                        log.warn("ReturnCallback: message returned. exchange={}, routingKey={}, replyText={}",
                                returnedMessage.getExchange(),
                                returnedMessage.getRoutingKey(),
                                returnedMessage.getReplyText());
                        // correlationId 可能为 null，需保护
                        Object corr = returnedMessage.getMessage().getMessageProperties().getCorrelationId();
                        String messageId = corr != null ? corr.toString() : null;
                        handleConfirm(messageId, false, "message_returned:" + returnedMessage.getReplyText());
                    } catch (Exception ex) {
                        log.error("处理返回消息时异常: {}", ex.getMessage(), ex);
                    }
                }
        );
    }

    /**
     * 发送私聊消息
     */
    @Override
    public Result<?> sendSingleMessage(IMSingleMessage dto) {
        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_KEY_SEND_SINGLE + dto.getFromId() + ":" + dto.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取发送私聊锁，from={} to={} tempId={}", dto.getFromId(), dto.getToId(), dto.getMessageTempId());
                return Result.failed("消息发送中，请稍后重试");
            }

            Long messageId = imIdGeneratorFeign.getId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id, Long.class);
            Long messageTime = DateTimeUtil.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            ImSingleMessagePo messagePo = new ImSingleMessagePo().setDelFlag(IMStatus.YES.getCode());
            BeanUtils.copyProperties(dto, messagePo);

            // 异步插入消息和更新会话
            CompletableFuture.runAsync(() -> {
                insertImSingleMessage(messagePo);
                createOrUpdateImChat(dto.getFromId(), dto.getToId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
                createOrUpdateImChat(dto.getToId(), dto.getFromId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
            }, asyncTaskExecutor);

            // 检查接收者在线状态
            Object redisObj = redisUtil.get(USER_CACHE_PREFIX + dto.getToId());
            if (ObjectUtil.isEmpty(redisObj)) {
                log.info("单聊目标不在线 from:{} to:{}", dto.getFromId(), dto.getToId());
                return Result.success(ResultCode.USER_OFFLINE.getMessage(), dto);
            }

            IMRegisterUser registerUser = JsonUtil.parseObject(redisObj, IMRegisterUser.class);
            String brokerId = registerUser.getBrokerId();

            // 创建 outbox 并发送到 MQ
            createOutbox(String.valueOf(messageId), JsonUtil.toJSONString(dto), "direct", "single.message." + dto.getToId(), messageTime);

            IMessageWrap<Object> wrapper = new IMessageWrap<>().setCode(IMessageType.SINGLE_MESSAGE.getCode()).setData(dto).setIds(List.of(dto.getToId()));

            publishToBroker(MQ_EXCHANGE_NAME, brokerId, JsonUtil.toJSONString(wrapper), String.valueOf(messageId));

            log.info("单聊消息发送成功 from:{} to:{}", dto.getFromId(), dto.getToId());

            return Result.success(ResultCode.SUCCESS.getMessage(), dto);

        } catch (Exception e) {
            log.error("单聊消息发送异常: {}", e.getMessage(), e);
            return Result.failed("发送消息失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("单聊消息总耗时: {}ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 发送群聊消息
     */
    @Override
    public Result<?> sendGroupMessage(IMGroupMessage dto) {
        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_KEY_SEND_GROUP + dto.getGroupId() + ":" + dto.getFromId() + ":" + dto.getMessageTempId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取发送群聊锁，groupId={} from={} tempId={}", dto.getGroupId(), dto.getFromId(), dto.getMessageTempId());
                return Result.failed("消息发送中，请稍后重试");
            }

            Long messageId = imIdGeneratorFeign.getId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id, Long.class);
            Long messageTime = DateTimeUtil.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setSequence(messageTime);

            // 异步插入群消息
            CompletableFuture.runAsync(() -> insertImGroupMessage(dto), asyncTaskExecutor);

            List<ImGroupMemberPo> members = imGroupFeign.getGroupMemberList(dto.getGroupId());
            if (CollectionUtil.isEmpty(members)) {
                log.warn("群:{} 没有成员，无法发送消息", dto.getGroupId());
                return Result.failed("群聊成员为空，无法发送消息");
            }

            // 过滤发送者，获取接收者
            List<String> toList = members.stream()
                    .filter(m -> !m.getMemberId().equals(dto.getFromId()))
                    .map(m -> USER_CACHE_PREFIX + m.getMemberId())
                    .collect(Collectors.toList());

            // 异步设置读状态和更新会话
            CompletableFuture.runAsync(() -> {
                setGroupReadStatus(String.valueOf(messageId), dto.getGroupId(), members);
                updateGroupChats(dto.getGroupId(), messageTime, members);
            }, asyncTaskExecutor);

            // 批量获取在线用户并按broker分组
            List<Object> userObjs = redisUtil.batchGet(toList);
            Map<String, List<String>> brokerMap = new HashMap<>();
            for (Object obj : userObjs) {
                if (ObjectUtil.isNotEmpty(obj)) {
                    IMRegisterUser user = JsonUtil.parseObject(obj, IMRegisterUser.class);
                    brokerMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
                }
            }

            // 分发到各broker
            for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
                String brokerId = entry.getKey();
                dto.setToList(entry.getValue());

                createOutbox(String.valueOf(messageId), JsonUtil.toJSONString(dto), "topic", "group.message." + dto.getGroupId(), messageTime);
                IMessageWrap<Object> wrapper = new IMessageWrap<>().setCode(IMessageType.GROUP_MESSAGE.getCode()).setData(dto).setIds(entry.getValue());
                publishToBroker(MQ_EXCHANGE_NAME, brokerId, JsonUtil.toJSONString(wrapper), String.valueOf(messageId));
            }

            return Result.success(ResultCode.SUCCESS.getMessage(), dto);

        } catch (Exception e) {
            log.error("群消息发送失败, 群ID: {}, 发送者: {}", dto.getGroupId(), dto.getFromId(), e);
            return Result.failed("发送群消息失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("群聊消息总耗时: {}ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 发送视频消息
     */
    @Override
    public Result<?> sendVideoMessage(IMVideoMessage videoMessage) {
        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_KEY_SEND_VIDEO + videoMessage.getFromId() + ":" + videoMessage.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取发送视频消息锁，from={} to={}", videoMessage.getFromId(), videoMessage.getToId());
                return Result.failed("消息发送中，请稍后重试");
            }

            if (videoMessage == null || videoMessage.getToId() == null) {
                return Result.failed("参数无效，消息或接收方为空");
            }

            Object redisObj = redisUtil.get(USER_CACHE_PREFIX + videoMessage.getToId());
            if (ObjectUtil.isEmpty(redisObj)) {
                log.info("用户 [{}] 未登录，消息发送失败", videoMessage.getToId());
                return Result.failed("用户未登录");
            }

            IMRegisterUser targetUser = JsonUtil.parseObject(redisObj, IMRegisterUser.class);
            String brokerId = targetUser.getBrokerId();

            IMessageWrap<Object> wrapMsg = new IMessageWrap<>().setCode(IMessageType.VIDEO_MESSAGE.getCode()).setData(videoMessage).setIds(List.of(videoMessage.getToId()));

            MessagePostProcessor mpp = msg -> {
                msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return msg;
            };

            rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME, brokerId, JsonUtil.toJSONString(wrapMsg), mpp);

            log.info("视频消息发送成功，from={} → to={} via broker={}", videoMessage.getFromId(), videoMessage.getToId(), brokerId);
            return Result.success("消息发送成功");
        } catch (Exception e) {
            log.error("发送视频消息异常，toId={}", videoMessage.getToId(), e);
            return Result.failed("消息发送异常");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.debug("视频消息总耗时: {}ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 撤回消息（Redisson锁已优化）
     */
    @Override
    public Result<?> recallMessage(IMessageAction dto) {
        long startTime = System.currentTimeMillis();
        try {
            if (dto == null || dto.getMessageId() == null || dto.getOperatorId() == null) {
                return Result.failed("参数无效");
            }

            String messageId = dto.getMessageId();
            String operatorId = dto.getOperatorId();
            Long recallTime = dto.getRecallTime() != null ? dto.getRecallTime() : DateTimeUtil.getCurrentUTCTimestamp();
            String reason = dto.getReason();

            Integer messageType = dto.getMessageType();
            if (messageType == null) {
                return Result.failed("无法确定消息类型");
            }

            // 使用分布式锁确保撤回操作的原子性
            String lockKey = LOCK_KEY_RECALL_MESSAGE + messageId;
            RLock lock = redissonClient.getLock(lockKey);
            
            try {
                // 尝试获取锁，等待3秒，持有锁10秒
                boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
                if (!acquired) {
                    log.warn("无法获取撤回消息的分布式锁，messageId={}", messageId);
                    return Result.failed("消息正在处理中，请稍后再试");
                }
                
                Map<String, Object> recallPayload = new HashMap<>();
                recallPayload.put("_recalled", true);
                recallPayload.put("operatorId", operatorId);
                recallPayload.put("recallTime", recallTime);
                recallPayload.put("reason", reason);

                if (messageType.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
                    ImSingleMessagePo msg = imMessageFeign.getSingleMessageById(messageId);
                    if (Objects.isNull(msg)) return Result.failed("消息不存在");
                    if (!operatorId.equals(msg.getFromId())) return Result.failed("无权撤回");

                    // 检查是否已撤回
                    Map<String, Object> body = safeParseMessageBody(msg.getMessageBody());
                    if (Boolean.TRUE.equals(body.get("_recalled"))) return Result.success("消息已撤回");

                    recallPayload.put("messageBody", msg.getMessageBody());
                    ImSingleMessagePo update = new ImSingleMessagePo().setMessageId(messageId).setMessageBody(JsonUtil.toJSONString(recallPayload)).setUpdateTime(recallTime);
                    imMessageFeign.singleMessageSaveOrUpdate(update);

                    // 广播给双方
                    broadcastRecall(dto, messageId, recallTime, List.of(msg.getFromId(), msg.getToId()), IMessageType.SINGLE_MESSAGE.getCode());
                }
                if (messageType.equals(IMessageType.GROUP_MESSAGE.getCode())) {
                    ImGroupMessagePo msg = imMessageFeign.getGroupMessageById(messageId);
                    if (Objects.isNull(msg)) return Result.failed("消息不存在");
                    if (!operatorId.equals(msg.getFromId())) return Result.failed("无权撤回");

                    // 权限校验（发送者或群主）
    //                if (!operatorId.equals(msg.getFromId())) {
    //                    boolean isAdmin = imGroupFeign.isGroupAdmin(msg.getGroupId(), operatorId);
    //                    if (!isAdmin) return Result.failed("无权撤回");
    //                }

                    Map<String, Object> body = safeParseMessageBody(msg.getMessageBody());
                    if (Boolean.TRUE.equals(body.get("_recalled"))) return Result.success("消息已撤回");

                    recallPayload.put("groupId", msg.getGroupId());
                    ImGroupMessagePo update = new ImGroupMessagePo().setMessageId(messageId).setMessageBody(JsonUtil.toJSONString(recallPayload)).setUpdateTime(recallTime);
                    imMessageFeign.groupMessageSaveOrUpdate(update);

                    // 广播给群成员
                    List<ImGroupMemberPo> members = imGroupFeign.getGroupMemberList(msg.getGroupId());
                    if (CollectionUtil.isNotEmpty(members)) {
                        List<String> memberIds = members.stream().map(ImGroupMemberPo::getMemberId).collect(Collectors.toList());
                        broadcastRecall(dto, messageId, recallTime, memberIds, IMessageType.GROUP_MESSAGE.getCode());
                    }
                }
            } finally {
                // 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

            return Result.success("消息撤回成功");
        } catch (Exception e) {
            log.error("撤回消息异常: {}", e.getMessage(), e);
            return Result.failed("撤回失败");
        } finally {
            log.info("撤回消息总耗时: {}ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 广播撤回通知
     */
    private void broadcastRecall(IMessageAction dto, String messageId, Long recallTime, List<String> recipientIds, Integer messageType) {
        dto.setMessageTime(recallTime).setMessageId(messageId);

        List<String> redisKeys = recipientIds.stream().map(id -> USER_CACHE_PREFIX + id).collect(Collectors.toList());
        List<Object> redisObjs = redisUtil.batchGet(redisKeys);

        Map<String, List<String>> brokerMap = new HashMap<>();
        for (Object obj : redisObjs) {
            if (ObjectUtil.isNotEmpty(obj)) {
                IMRegisterUser user = JsonUtil.parseObject(obj, IMRegisterUser.class);
                brokerMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
            }
        }

        for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
            IMessageWrap<Object> wrap = new IMessageWrap<>().setCode(messageType).setData(dto).setIds(entry.getValue());
            publishToBroker(MQ_EXCHANGE_NAME, entry.getKey(), JsonUtil.toJSONString(wrap), messageId);
        }

        log.info("撤回完成 messageId={} type={}", messageId, messageType);
    }

    /**
     * 获取消息列表
     */
    @Override
    public Map<Integer, Object> list(ChatDto chatDto) {
        String userId = chatDto.getFromId();
        Long sequence = chatDto.getSequence();

        Map<Integer, Object> map = new HashMap<>();

        List<ImSingleMessagePo> singleMessages = imMessageFeign.getSingleMessageList(userId, sequence);
        if (!CollectionUtil.isEmpty(singleMessages)) {
            map.put(IMessageType.SINGLE_MESSAGE.getCode(), singleMessages);
        }

        List<ImGroupMessagePo> groupMessages = imMessageFeign.getGroupMessageList(userId, sequence);
        if (!CollectionUtil.isEmpty(groupMessages)) {
            map.put(IMessageType.GROUP_MESSAGE.getCode(), groupMessages);
        }

        return map;
    }

    /**
     * 插入私聊消息
     */
    private void insertImSingleMessage(ImSingleMessagePo messagePo) {
        try {
            if (!imMessageFeign.singleMessageSaveOrUpdate(messagePo)) {
                log.error("保存私聊消息失败 messageId={}", messagePo.getMessageId());
            }
        } catch (Exception e) {
            log.error("插入私聊消息异常 messageId={}", messagePo.getMessageId(), e);
        }
    }

    /**
     * 插入群聊消息
     */
    private void insertImGroupMessage(IMGroupMessage dto) {
        try {
            ImGroupMessagePo po = new ImGroupMessagePo().setDelFlag(IMStatus.YES.getCode());
            BeanUtils.copyProperties(dto, po);
            if (!imMessageFeign.groupMessageSaveOrUpdate(po)) {
                log.error("保存群消息失败 messageId={}", dto.getMessageId());
            }
        } catch (Exception e) {
            log.error("插入群消息异常 messageId={}", dto.getMessageId(), e);
        }
    }

    /**
     * 创建或更新会话（通用）
     */
    private void createOrUpdateImChat(String ownerId, String toId, Long messageTime, Integer chatType) {
        try {
            ImChatPo chatPo = imChatFeign.getOne(ownerId, toId, chatType);
            if (Objects.isNull(chatPo)) {
                chatPo = new ImChatPo()
                        .setChatId(imIdGeneratorFeign.getId(IdGeneratorConstant.uuid, IdGeneratorConstant.chat_id, String.class))
                        .setOwnerId(ownerId)
                        .setToId(toId)
                        .setSequence(messageTime)
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setDelFlag(IMStatus.YES.getCode())
                        .setChatType(chatType);
                if (!imChatFeign.insert(chatPo)) {
                    log.error("保存会话失败 ownerId={} toId={}", ownerId, toId);
                }
            } else {
                chatPo.setSequence(messageTime);
                if (!imChatFeign.updateById(chatPo)) {
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
            imMessageFeign.groupMessageStatusBatchInsert(statusList);
        } catch (Exception e) {
            log.error("设置群读状态失败 messageId={}", messageId, e);
        }
    }

    /**
     * 创建Outbox
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

        CompletableFuture.runAsync(() -> {
            if (!imOutboxFeign.saveOrUpdate(po)) {
                log.warn("Outbox保存失败 messageId={}", messageId);
            }
        }, asyncTaskExecutor);

        messageToOutboxId.put(messageId, outboxId);
    }

    /**
     * 发布到Broker
     */
    private void publishToBroker(String exchange, String routingKey, String payload, String messageId) {
        CompletableFuture.runAsync(() -> {
            try {
                MessagePostProcessor mpp = msg -> {
                    msg.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    msg.getMessageProperties()
                            .setCorrelationId(messageId);
                    return msg;
                };
                CorrelationData corr = new CorrelationData(messageId);
                rabbitTemplate.convertAndSend(exchange, routingKey, payload, mpp, corr);
            } catch (Exception e) {
                log.error("发布失败 messageId={}", messageId, e);
                Long outboxId = messageToOutboxId.get(messageId);
                if (outboxId != null) {
                    imOutboxFeign.markAsFailed(outboxId, e.getMessage(), 1);
                }
            }
        }, asyncTaskExecutor);
    }

    /**
     * 处理确认回调
     */
    private void handleConfirm(String messageId, boolean ack, String cause) {
        if (messageId == null) return;
        Long outboxId = messageToOutboxId.get(messageId);
        if (outboxId == null) return;

        try {
            if (ack) {
                imOutboxFeign.updateStatus(outboxId, "SENT", 1);
                messageToOutboxId.remove(messageId);
            } else {
                imOutboxFeign.updateStatus(outboxId, "PENDING", 0);
            }
        } catch (Exception e) {
            log.error("更新Outbox失败 outboxId={} messageId={}", outboxId, messageId, e);
        }
    }

    /**
     * 重试Pending消息（加Redisson锁防并发重试）
     */
    public void retryPendingMessages() {
        long startTime = System.currentTimeMillis();
        RLock lock = redissonClient.getLock(LOCK_KEY_RETRY_PENDING);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取重试Pending锁");
                return;
            }

            List<IMOutboxPo> pending = imOutboxFeign.listByStatus("PENDING", 100);
            if (CollectionUtil.isEmpty(pending)) return;

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
                    imOutboxFeign.updateStatus(o.getId(), "SENT", attempts);
                } catch (Exception e) {
                    log.error("重试失败 messageId={}", o.getMessageId(), e);
                    imOutboxFeign.markAsFailed(o.getId(), e.getMessage(), attempts);
                }
            }
        } catch (Exception e) {
            log.error("重试任务失败", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("重试任务总耗时: {}ms", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 安全解析MessageBody
     */
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
}