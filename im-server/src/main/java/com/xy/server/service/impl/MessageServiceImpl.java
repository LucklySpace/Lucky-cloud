package com.xy.server.service.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.core.enums.IMStatus;
import com.xy.core.enums.IMWebRTCType;
import com.xy.core.enums.IMessageReadStatus;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.*;
import com.xy.domain.dto.ChatDto;
import com.xy.domain.mapper.MessageBeanMapper;
import com.xy.domain.po.*;
import com.xy.dubbo.api.database.chat.ImChatDubboService;
import com.xy.dubbo.api.database.group.ImGroupMemberDubboService;
import com.xy.dubbo.api.database.message.ImGroupMessageDubboService;
import com.xy.dubbo.api.database.message.ImSingleMessageDubboService;
import com.xy.dubbo.api.database.outbox.IMOutboxDubboService;
import com.xy.dubbo.api.id.ImIdDubboService;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.mq.rabbit.core.RabbitTemplateFactory;
import com.xy.server.api.IdGeneratorConstant;
import com.xy.server.service.MessageService;
import com.xy.server.utils.RedisUtil;
import com.xy.utils.json.JacksonUtils;
import com.xy.utils.time.DateTimeUtils;
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
import org.springframework.util.StopWatch;

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
    private static final String LOCK_KEY_SEND_SINGLE = "lock:send:single:";
    private static final String LOCK_KEY_SEND_GROUP = "lock:send:group:";
    private static final String LOCK_KEY_SEND_VIDEO = "lock:send:video:";
    private static final String LOCK_KEY_RECALL_MESSAGE = "recall:message:lock:";
    private static final String LOCK_KEY_RETRY_PENDING = "lock:retry:pending";
    private static final long LOCK_WAIT_TIME = 5L; // 锁等待5s
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有10s
    private final Map<String, Long> messageToOutboxIdMap = new ConcurrentHashMap<>();

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
        StopWatch stopWatch = new StopWatch("sendSingleMessage");
        stopWatch.start("overall");

        String lockKey = LOCK_KEY_SEND_SINGLE + dto.getFromId() + ":" + dto.getToId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            stopWatch.stop();
            stopWatch.start("lockAcquire");
            boolean locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);
            stopWatch.stop();

            if (!locked) {
                log.warn("无法获取发送私聊锁，from={} to={} tempId={} (lockWait={}s)",
                        dto.getFromId(), dto.getToId(), dto.getMessageTempId(), LOCK_WAIT_TIME);
                stopWatch.start("logTiming");
                stopWatch.stop();
                log.info("sendSingleMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("消息发送中，请稍后重试");
            }

            // id 生成
            stopWatch.start("idGeneration");
            Long messageId = imIdDubboService.generateId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id).getLongId();
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();
            stopWatch.stop();

            // dto 填充
            stopWatch.start("dtoSetup");
            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);
            stopWatch.stop();

            // bean copy
            stopWatch.start("beanCopy");
            ImSingleMessagePo messagePo = MessageBeanMapper.INSTANCE.toImSingleMessagePo(dto);
            messagePo.setDelFlag(IMStatus.YES.getCode());
            stopWatch.stop();

            // 异步插入消息和更新会话
            stopWatch.start("asyncSchedule");
            CompletableFuture.runAsync(() -> {
                createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "single.message." + dto.getToId(), messageTime);
                insertImSingleMessage(messagePo);
                createOrUpdateImChat(dto.getFromId(), dto.getToId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
                createOrUpdateImChat(dto.getToId(), dto.getFromId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
            }, asyncTaskExecutor);
            stopWatch.stop();

            // 检查接收者在线状态（Redis）
            stopWatch.start("redisCheck");
            IMRegisterUser redisObj = redisUtil.get(USER_CACHE_PREFIX + dto.getToId());
            stopWatch.stop();

            if (Objects.isNull(redisObj)) {
                log.info("单聊目标不在线 from:{} to:{}", dto.getFromId(), dto.getToId());
                stopWatch.start("logTiming");
                stopWatch.stop();
                log.info("sendSingleMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.success(ResultCode.USER_OFFLINE.getMessage(), dto);
            }

            // 包装消息并序列化
            stopWatch.start("serialization");
            IMessageWrap<Object> wrapper = new IMessageWrap<>().setCode(IMessageType.SINGLE_MESSAGE.getCode()).setData(dto).setIds(List.of(dto.getToId()));
            String payload = JacksonUtils.toJSONString(wrapper);
            stopWatch.stop();

            // 发布到 broker
            stopWatch.start("publishToBroker");
            publishToBroker(MQ_EXCHANGE_NAME, redisObj.getBrokerId(), payload, String.valueOf(messageId));
            stopWatch.stop();

            stopWatch.start("logSuccess");
            log.info("单聊消息发送成功 from:{} to:{}", dto.getFromId(), dto.getToId());
            stopWatch.stop();
            stopWatch.start("logTiming");
            stopWatch.stop();
            log.info("sendSingleMessage timing summary: {}", stopWatch.prettyPrint());
            return Result.success(ResultCode.SUCCESS.getMessage(), dto);

        } catch (Exception e) {
            log.error("单聊消息发送异常: {}", e.getMessage(), e);
            return Result.failed("发送消息失败");
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception ex) {
                log.warn("unlock error", ex);
            }
            log.info("sendSingleMessage timing summary: {}", stopWatch.prettyPrint());
        }
    }

    /**
     * 发送群聊消息
     */
    @Override
    public Result<?> sendGroupMessage(IMGroupMessage dto) {
        StopWatch stopWatch = new StopWatch("sendGroupMessage");
        stopWatch.start("overall");

        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_KEY_SEND_GROUP + dto.getGroupId() + ":" + dto.getFromId() + ":" + dto.getMessageTempId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            stopWatch.stop();
            stopWatch.start("lockAcquire");
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取发送群聊锁，groupId={} from={} tempId={}", dto.getGroupId(), dto.getFromId(), dto.getMessageTempId());
                stopWatch.stop();
                log.info("sendGroupMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("消息发送中，请稍后重试");
            }
            stopWatch.stop();

            stopWatch.start("idGeneration");
            Long messageId = imIdDubboService.generateId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id).getLongId();
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();
            stopWatch.stop();

            stopWatch.start("dtoSetup");
            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setSequence(messageTime);
            stopWatch.stop();

            stopWatch.start("getGroupMembers");
            List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(dto.getGroupId());
            stopWatch.stop();

            if (CollectionUtils.isEmpty(members)) {
                log.warn("群:{} 没有成员，无法发送消息", dto.getGroupId());
                stopWatch.start("logTiming");
                stopWatch.stop();
                log.info("sendGroupMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("群聊成员为空，无法发送消息");
            }

            // 过滤发送者，获取接收者
            stopWatch.start("filterRecipients");
            List<String> toList = members.stream()
                    .filter(m -> !m.getMemberId().equals(dto.getFromId()))
                    .map(m -> USER_CACHE_PREFIX + m.getMemberId())
                    .collect(Collectors.toList());
            stopWatch.stop();

            // 异步插入群消息 异步设置读状态和更新会话
            stopWatch.start("asyncSchedule");
            CompletableFuture.runAsync(() -> {
                createOutbox(String.valueOf(messageId), JacksonUtils.toJSONString(dto), MQ_EXCHANGE_NAME, "group.message." + dto.getGroupId(), messageTime);
                insertImGroupMessage(dto);
                setGroupReadStatus(String.valueOf(messageId), dto.getGroupId(), members);
                updateGroupChats(dto.getGroupId(), messageTime, members);
            }, asyncTaskExecutor);
            stopWatch.stop();

            // 批量获取在线用户并按broker分组
            stopWatch.start("batchGetUsers");
            List<Object> userObjs = redisUtil.batchGet(toList);
            Map<String, List<String>> brokerMap = new HashMap<>();
            for (Object obj : userObjs) {
                if (Objects.nonNull(obj)) {
                    IMRegisterUser user = JacksonUtils.parseObject(obj, IMRegisterUser.class);
                    if (user != null) {
                        brokerMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
                    }
                }
            }
            stopWatch.stop();

            // 分发到各broker
            stopWatch.start("publishToBrokers");
            for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
                String brokerId = entry.getKey();
                dto.setToList(entry.getValue());
                IMessageWrap<Object> wrapper = new IMessageWrap<>().setCode(IMessageType.GROUP_MESSAGE.getCode()).setData(dto).setIds(entry.getValue());
                publishToBroker(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapper), String.valueOf(messageId));
            }
            stopWatch.stop();

            stopWatch.start("logTiming");
            stopWatch.stop();
            log.info("sendGroupMessage timing summary: {}", stopWatch.prettyPrint());
            return Result.success(ResultCode.SUCCESS.getMessage(), dto);

        } catch (Exception e) {
            log.error("群消息发送失败, 群ID: {}, 发送者: {}", dto.getGroupId(), dto.getFromId(), e);
            return Result.failed("发送群消息失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("群聊消息总耗时: {}ms", System.currentTimeMillis() - startTime);
            log.info("sendGroupMessage timing summary: {}", stopWatch.prettyPrint());
        }
    }

    /**
     * 发送视频消息（根据类型判断是否发送文本消息）
     */
    @Override
    public Result<?> sendVideoMessage(IMVideoMessage videoMessage) {
        StopWatch stopWatch = new StopWatch("sendVideoMessage");
        stopWatch.start("overall");

        long startTime = System.currentTimeMillis();
        String lockKey = LOCK_KEY_SEND_VIDEO + videoMessage.getFromId() + ":" + videoMessage.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            stopWatch.stop();
            stopWatch.start("lockAcquire");
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取发送视频锁，from={} to={}", videoMessage.getFromId(), videoMessage.getToId());
                stopWatch.stop();
                log.info("sendVideoMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("视频发送中，请稍后重试");
            }
            stopWatch.stop();

            stopWatch.start("validateParams");
            if (Objects.isNull(videoMessage.getFromId()) || Objects.isNull(videoMessage.getToId())) {
                stopWatch.stop();
                log.info("sendVideoMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("参数无效，消息发送方或接收方为空");
            }
            stopWatch.stop();

            // 检查接收者在线状态
            stopWatch.start("checkOnlineStatus");
            Object redisObj = redisUtil.get(USER_CACHE_PREFIX + videoMessage.getToId());
            stopWatch.stop();

            if (Objects.isNull(redisObj)) {
                log.info("用户 [{}] 未登录，消息发送失败", videoMessage.getToId());
                stopWatch.start("logTiming");
                stopWatch.stop();
                log.info("sendVideoMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("用户未登录");
            }

            stopWatch.start("parseTargetUser");
            IMRegisterUser targetUser = JacksonUtils.parseObject(redisObj, IMRegisterUser.class);
            String brokerId = targetUser.getBrokerId();
            stopWatch.stop();

            stopWatch.start("getWebRTCType");
            int typeCode = videoMessage.getType(); // 假设IMVideoMessage有getType()返回code
            IMWebRTCType webRTCType = IMWebRTCType.getByCode(typeCode); // 假设枚举有fromCode静态方法
            stopWatch.stop();

            if (webRTCType == null) {
                log.warn("未知WebRTC类型 code={}", typeCode);
                stopWatch.start("logTiming");
                stopWatch.stop();
                log.info("sendVideoMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("无效消息类型");
            }

            // 对于特定类型，发送文本消息
//            if (webRTCType == IMWebRTCType.RTC_REJECT ||
//                    webRTCType == IMWebRTCType.RTC_CANCEL ||
//                    webRTCType == IMWebRTCType.RTC_FAILED ||
//                    webRTCType == IMWebRTCType.RTC_HANDUP) {
//
//                // 构建文本消息
//                IMSingleMessage textMessage = new IMSingleMessage();
//                textMessage.setFromId(videoMessage.getFromId());
//                textMessage.setToId(videoMessage.getToId());
//                textMessage.setMessageContentType(IMessageContentType.TEXT.getCode()); // 假设文本类型code
//                String textContent = webRTCType.getDesc(); // 使用枚举desc，或自定义如"用户拒绝了通话"
//                textMessage.setMessageBody(new IMessage.TextMessageBody().setText(textContent));
//
//                // 发送文本消息
//                return sendSingleMessage(textMessage);
//            }

            // 其他类型发送视频消息
            stopWatch.start("wrapAndSend");
            IMessageWrap<Object> wrapMsg = new IMessageWrap<>().setCode(IMessageType.VIDEO_MESSAGE.getCode()).setData(videoMessage).setIds(List.of(videoMessage.getToId()));

            MessagePostProcessor mpp = msg -> {
                msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return msg;
            };

            rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME, brokerId, JacksonUtils.toJSONString(wrapMsg), mpp);
            stopWatch.stop();

            stopWatch.start("logSuccess");
            log.info("视频消息发送成功，from={} → to={} via broker={}", videoMessage.getFromId(), videoMessage.getToId(), brokerId);
            stopWatch.stop();
            stopWatch.start("logTiming");
            stopWatch.stop();
            log.info("sendVideoMessage timing summary: {}", stopWatch.prettyPrint());
            return Result.success("消息发送成功");
        } catch (Exception e) {
            log.error("发送视频消息异常，toId={}", videoMessage.getToId(), e);
            return Result.failed("消息发送异常");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.debug("视频消息总耗时: {}ms", System.currentTimeMillis() - startTime);
            log.info("sendVideoMessage timing summary: {}", stopWatch.prettyPrint());
        }
    }

    /**
     * 撤回消息（Redisson锁已优化）
     */
    @Override
    public Result<?> recallMessage(IMessageAction dto) {
        StopWatch stopWatch = new StopWatch("recallMessage");
        stopWatch.start("overall");

        long startTime = System.currentTimeMillis();
        try {
            stopWatch.stop();
            stopWatch.start("validateParams");
            if (dto == null || dto.getMessageId() == null || dto.getOperatorId() == null) {
                stopWatch.stop();
                log.info("recallMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("参数无效");
            }
            stopWatch.stop();

            stopWatch.start("extractParams");
            String messageId = dto.getMessageId();
            String operatorId = dto.getOperatorId();
            Long recallTime = dto.getRecallTime() != null ? dto.getRecallTime() : DateTimeUtils.getCurrentUTCTimestamp();
            String reason = dto.getReason();

            Integer messageType = dto.getMessageType();
            if (messageType == null) {
                stopWatch.stop();
                log.info("recallMessage timing summary: {}", stopWatch.prettyPrint());
                return Result.failed("无法确定消息类型");
            }
            stopWatch.stop();

            // 使用分布式锁确保撤回操作的原子性
            stopWatch.start("acquireLock");
            String lockKey = LOCK_KEY_RECALL_MESSAGE + messageId;
            RLock lock = redissonClient.getLock(lockKey);

            try {
                // 尝试获取锁，等待3秒，持有锁10秒
                boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
                stopWatch.stop();
                if (!acquired) {
                    log.warn("无法获取撤回消息的分布式锁，messageId={}", messageId);
                    stopWatch.start("logTiming");
                    stopWatch.stop();
                    log.info("recallMessage timing summary: {}", stopWatch.prettyPrint());
                    return Result.failed("消息正在处理中，请稍后再试");
                }

                stopWatch.start("prepareRecallPayload");
                Map<String, Object> recallPayload = new HashMap<>();
                recallPayload.put("_recalled", true);
                recallPayload.put("operatorId", operatorId);
                recallPayload.put("recallTime", recallTime);
                recallPayload.put("reason", reason);
                stopWatch.stop();

                if (messageType.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
                    stopWatch.start("processSingleMessage");
                    ImSingleMessagePo msg = imSingleMessageDubboService.selectOne(messageId);
                    if (Objects.isNull(msg)) return Result.failed("消息不存在");
                    if (!operatorId.equals(msg.getFromId())) return Result.failed("无权撤回");

                    // 检查是否已撤回
                    Map<String, Object> body = safeParseMessageBody(msg.getMessageBody());
                    if (Boolean.TRUE.equals(body.get("_recalled"))) return Result.success("消息已撤回");

                    recallPayload.put("messageBody", msg.getMessageBody());
                    ImSingleMessagePo update = new ImSingleMessagePo().setMessageId(messageId).setMessageBody(JacksonUtils.toJSONString(recallPayload)).setUpdateTime(recallTime);
                    imSingleMessageDubboService.update(update);

                    // 广播给双方
                    broadcastRecall(dto, messageId, recallTime, List.of(msg.getFromId(), msg.getToId()), IMessageType.SINGLE_MESSAGE.getCode());
                    stopWatch.stop();
                }
                if (messageType.equals(IMessageType.GROUP_MESSAGE.getCode())) {
                    stopWatch.start("processGroupMessage");
                    ImGroupMessagePo msg = imGroupMessageDubboService.selectOne(messageId);
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
                    ImGroupMessagePo update = new ImGroupMessagePo().setMessageId(messageId).setMessageBody(JacksonUtils.toJSONString(recallPayload)).setUpdateTime(recallTime);
                    imGroupMessageDubboService.update(update);

                    // 广播给群成员
                    List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(msg.getGroupId());
                    if (!CollectionUtils.isEmpty(members)) {
                        List<String> memberIds = members.stream().map(ImGroupMemberPo::getMemberId).collect(Collectors.toList());
                        broadcastRecall(dto, messageId, recallTime, memberIds, IMessageType.GROUP_MESSAGE.getCode());
                    }
                    stopWatch.stop();
                }
            } finally {
                // 释放锁
                stopWatch.start("releaseLock");
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
                stopWatch.stop();
            }

            stopWatch.start("logTiming");
            stopWatch.stop();
            log.info("recallMessage timing summary: {}", stopWatch.prettyPrint());
            return Result.success("消息撤回成功");
        } catch (Exception e) {
            log.error("撤回消息异常: {}", e.getMessage(), e);
            return Result.failed("撤回失败");
        } finally {
            log.info("撤回消息总耗时: {}ms", System.currentTimeMillis() - startTime);
            log.info("recallMessage timing summary: {}", stopWatch.prettyPrint());
        }
    }

    /**
     * 广播撤回通知
     */
    private void broadcastRecall(IMessageAction dto, String messageId, Long recallTime, List<String> recipientIds, Integer messageType) {
        StopWatch stopWatch = new StopWatch("broadcastRecall");
        stopWatch.start("overall");

        dto.setMessageTime(recallTime).setMessageId(messageId);

        stopWatch.stop();
        stopWatch.start("batchGetUsers");
        List<String> redisKeys = recipientIds.stream().map(id -> USER_CACHE_PREFIX + id).collect(Collectors.toList());
        List<Object> redisObjs = redisUtil.batchGet(redisKeys);
        stopWatch.stop();

        stopWatch.start("groupUsersByBroker");
        Map<String, List<String>> brokerMap = new HashMap<>();
        for (Object obj : redisObjs) {
            if (Objects.nonNull(obj)) {
                IMRegisterUser user = JacksonUtils.parseObject(obj, IMRegisterUser.class);
                brokerMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
            }
        }
        stopWatch.stop();

        stopWatch.start("publishToBrokers");
        for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
            IMessageWrap<Object> wrap = new IMessageWrap<>().setCode(messageType).setData(dto).setIds(entry.getValue());
            publishToBroker(MQ_EXCHANGE_NAME, entry.getKey(), JacksonUtils.toJSONString(wrap), messageId);
        }
        stopWatch.stop();

        stopWatch.start("logTiming");
        stopWatch.stop();
        log.info("broadcastRecall timing summary: {}", stopWatch.prettyPrint());
        log.info("撤回完成 messageId={} type={}", messageId, messageType);
    }

    /**
     * 获取消息列表
     */
    @Override
    public Map<Integer, Object> list(ChatDto chatDto) {
        StopWatch stopWatch = new StopWatch("listMessages");
        stopWatch.start("overall");

        String userId = chatDto.getFromId();
        Long sequence = chatDto.getSequence();

        Map<Integer, Object> map = new HashMap<>();

        stopWatch.stop();
        stopWatch.start("getSingleMessages");
        List<ImSingleMessagePo> singleMessages = imSingleMessageDubboService.selectList(userId, sequence);
        if (!CollectionUtils.isEmpty(singleMessages)) {
            map.put(IMessageType.SINGLE_MESSAGE.getCode(), singleMessages);
        }
        stopWatch.stop();

        stopWatch.start("getGroupMessages");
        List<ImGroupMessagePo> groupMessages = imGroupMessageDubboService.selectList(userId, sequence);
        if (!CollectionUtils.isEmpty(groupMessages)) {
            map.put(IMessageType.GROUP_MESSAGE.getCode(), groupMessages);
        }
        stopWatch.stop();

        stopWatch.start("logTiming");
        stopWatch.stop();
        log.info("listMessages timing summary: {}", stopWatch.prettyPrint());
        return map;
    }

    /**
     * 插入私聊消息
     */
    private void insertImSingleMessage(ImSingleMessagePo messagePo) {
        try {
            if (!imSingleMessageDubboService.insert(messagePo)) {
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
            ImGroupMessagePo po = MessageBeanMapper.INSTANCE.toImGroupMessagePo(dto);
            po.setDelFlag(IMStatus.YES.getCode());
            if (!imGroupMessageDubboService.insert(po)) {
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
                    log.error("保存会话失败 ownerId={} toId={}", ownerId, toId);
                }
            } else {
                chatPo.setSequence(messageTime);
                if (!imChatDubboService.update(chatPo)) {
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
            imGroupMessageDubboService.batchInsert(statusList);
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

        if (!imOutboxDubboService.saveOrUpdate(po)) {
            log.warn("Outbox保存失败 messageId={}", messageId);
        } else {
            messageToOutboxIdMap.put(messageId, outboxId);
        }
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
                Long outboxId = messageToOutboxIdMap.get(messageId);
                if (outboxId != null) {
                    imOutboxDubboService.markAsFailed(outboxId, e.getMessage(), 1);
                }
            }
        }, asyncTaskExecutor);
    }

    /**
     * 处理确认回调
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
            log.error("更新Outbox失败 outboxId={} messageId={}", outboxId, messageId, e);
        }
    }

    /**
     * 重试Pending消息（加Redisson锁防并发重试）
     */
    public void retryPendingMessages() {
        StopWatch stopWatch = new StopWatch("retryPendingMessages");
        stopWatch.start("overall");

        long startTime = System.currentTimeMillis();
        RLock lock = redissonClient.getLock(LOCK_KEY_RETRY_PENDING);
        try {
            stopWatch.stop();
            stopWatch.start("acquireLock");
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取重试Pending锁");
                stopWatch.stop();
                log.info("retryPendingMessages timing summary: {}", stopWatch.prettyPrint());
                return;
            }
            stopWatch.stop();

            stopWatch.start("getPendingMessages");
            List<IMOutboxPo> pending = imOutboxDubboService.listByStatus("PENDING", 100);
            stopWatch.stop();

            if (CollectionUtils.isEmpty(pending)) {
                stopWatch.start("logTiming");
                stopWatch.stop();
                log.info("retryPendingMessages timing summary: {}", stopWatch.prettyPrint());
                return;
            }

            stopWatch.start("retryMessages");
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
                    log.error("重试失败 messageId={}", o.getMessageId(), e);
                    imOutboxDubboService.markAsFailed(o.getId(), e.getMessage(), attempts);
                }
            }
            stopWatch.stop();
        } catch (Exception e) {
            log.error("重试任务失败", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            log.info("重试任务总耗时: {}ms", System.currentTimeMillis() - startTime);
            stopWatch.start("logTiming");
            stopWatch.stop();
            log.info("retryPendingMessages timing summary: {}", stopWatch.prettyPrint());
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
                return jacksonMapper.readValue(s, new TypeReference<>() {
                });
            } catch (Exception e) {
                return Map.of("raw", s);
            }
        }
        try {
            return jacksonMapper.convertValue(raw, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of("raw", String.valueOf(raw));
        }
    }
}