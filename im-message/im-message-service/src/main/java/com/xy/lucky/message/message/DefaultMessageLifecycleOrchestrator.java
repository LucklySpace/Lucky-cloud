package com.xy.lucky.message.message;

import com.xy.lucky.message.message.dispatch.MessageDispatchTask;
import com.xy.lucky.message.message.dispatch.LightweightTimeWheel;
import com.xy.lucky.message.message.monitor.MessageMetricsRecorder;
import com.xy.lucky.message.message.offline.OfflineMessageRecord;
import com.xy.lucky.message.message.offline.OfflineMessageService;
import com.xy.lucky.message.message.outbox.OutboxRecordService;
import com.xy.lucky.message.message.status.MessageStatusService;
import com.xy.lucky.message.utils.RedisUtil;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.mq.rabbit.core.RabbitTemplateFactory;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.json.JacksonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认消息生命周期编排器。
 * 统一承接消息实时分发、在线推送、离线存储补发、状态同步确认、重试失败处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultMessageLifecycleOrchestrator implements MessageLifecycleOrchestrator {

    private final RedisUtil redisUtil;
    private final RabbitTemplateFactory rabbitTemplateFactory;
    private final MessageStatusService messageStatusService;
    private final OfflineMessageService offlineMessageService;
    private final OutboxRecordService outboxRecordService;
    private final MessageMetricsRecorder messageMetricsRecorder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BlockingQueue<MessageDispatchTask> dispatchQueue = new LinkedBlockingQueue<>(10000);
    private final Map<String, MessageDispatchTask> pendingTaskMap = new ConcurrentHashMap<>();

    @Resource(name = "messagePushExecutor")
    private ExecutorService messagePushExecutor;

    @Resource(name = "scheduledExecutor")
    private ScheduledExecutorService scheduledExecutor;

    @Value("${message.dispatch.max-retry:3}")
    private int maxRetry;

    @Value("${message.dispatch.retry-delay-ms:1000}")
    private long retryDelayMs;

    @Value("${message.dispatch.worker-size:4}")
    private int workerSize;

    @Value("${message.dispatch.retry-wheel.tick-ms:100}")
    private long retryWheelTickMs;

    @Value("${message.dispatch.retry-wheel.slots:512}")
    private int retryWheelSlots;

    private RabbitTemplate rabbitTemplate;
    private LightweightTimeWheel retryTimeWheel;

    /**
     * 初始化分发器，构建 MQ 模板并启动任务消费循环。
     */
    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate(this::handleConfirm, this::handleReturn);
        messageMetricsRecorder.bindDispatchQueue(dispatchQueue);
        retryTimeWheel = new LightweightTimeWheel(retryWheelTickMs, retryWheelSlots, scheduledExecutor, messagePushExecutor);
        retryTimeWheel.start();
        scheduledExecutor.scheduleWithFixedDelay(this::refreshConnectionCount, 0, 10, TimeUnit.SECONDS);
        int size = Math.max(1, workerSize);
        for (int i = 0; i < size; i++) {
            messagePushExecutor.execute(this::dispatchLoop);
        }
        log.info("消息生命周期编排器启动完成: workerSize={}, maxRetry={}, retryDelayMs={}, retryWheelTickMs={}, retryWheelSlots={}",
                size, maxRetry, retryDelayMs, retryWheelTickMs, retryWheelSlots);
    }

    /**
     * 关闭编排器时主动停止时间轮，确保线程和任务可回收。
     */
    @PreDestroy
    public void destroy() {
        if (retryTimeWheel != null) {
            retryTimeWheel.stop();
        }
    }

    /**
     * 将业务消息按在线状态拆分并发送。
     *
     * @param messageType   消息类型编码
     * @param payload       原始业务消息体
     * @param targetUserIds 目标用户集合
     * @param messageId     全局消息 ID
     */
    @Override
    public void dispatch(Integer messageType, Object payload, Collection<String> targetUserIds, String messageId) {
        // 1.参数校验
        if (CollectionUtils.isEmpty(targetUserIds) || !StringUtils.hasText(messageId)) {
            return;
        }
        // 2.去重用户 ID
        List<String> deduplicatedUserIds = targetUserIds.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(deduplicatedUserIds)) {
            return;
        }
        // 3.标记消息为待处理
        messageStatusService.markPending(messageId, deduplicatedUserIds);
        // 4.获取用户路由
        RoutingPlan plan = resolveRoutingPlan(deduplicatedUserIds);

        // 5.使用队列消费消息
        for (Map.Entry<String, List<String>> entry : plan.onlineBrokerUsers.entrySet()) {
            IMessageWrap<Object> wrapper = new IMessageWrap<>()
                    .setCode(messageType)
                    .setData(payload)
                    .setIds(entry.getValue());
            String payloadJson = JacksonUtils.toJSONString(wrapper);
            Long outboxId = outboxRecordService.createPending(
                    messageId,
                    payloadJson,
                    IMConstant.MQ_EXCHANGE_NAME,
                    entry.getKey()
            );
            String correlationId = buildCorrelationId(messageId, entry.getKey());
            MessageDispatchTask task = MessageDispatchTask.builder()
                    .correlationId(correlationId)
                    .messageId(messageId)
                    .outboxId(outboxId)
                    .brokerId(entry.getKey())
                    .userIds(entry.getValue())
                    .payload(payloadJson)
                    .attempt(0)
                    .firstEnqueueAt(System.currentTimeMillis())
                    .build();
            offerDispatch(task);
            messageMetricsRecorder.onDispatchCreated();
        }
        // 6.存储离线消息
        for (String offlineUserId : plan.offlineUsers) {
            IMessageWrap<Object> wrapper = new IMessageWrap<>()
                    .setCode(messageType)
                    .setData(payload)
                    .setIds(List.of(offlineUserId));
            offlineMessageService.store(offlineUserId, OfflineMessageRecord.builder()
                    .messageId(messageId)
                    .messageType(messageType)
                    .payload(JacksonUtils.toJSONString(wrapper))
                    .build());
        }
    }

    /**
     * 记录客户端 ACK，驱动消息状态从投递完成推进到已确认。
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     */
    @Override
    public void acknowledge(String messageId, String userId) {
        if (!StringUtils.hasText(messageId) || !StringUtils.hasText(userId)) {
            return;
        }
        messageStatusService.acknowledge(messageId, userId);
        outboxRecordService.markDeliveredByMessageId(messageId);
    }

    /**
     * 按用户重放离线消息，在用户重新上线后补发。
     *
     * @param userId 用户 ID
     */
    @Override
    public void replayOfflineMessages(String userId) {
        if (!StringUtils.hasText(userId)) {
            return;
        }
        IMRegisterUser onlineUser = getOnlineUser(userId);
        if (onlineUser == null || !StringUtils.hasText(onlineUser.getBrokerId())) {
            return;
        }
        List<OfflineMessageRecord> records = offlineMessageService.pull(userId, 200);
        for (OfflineMessageRecord record : records) {
            Long outboxId = outboxRecordService.createPending(
                    record.messageId(),
                    record.payload(),
                    IMConstant.MQ_EXCHANGE_NAME,
                    onlineUser.getBrokerId()
            );
            MessageDispatchTask task = MessageDispatchTask.builder()
                    .correlationId(buildCorrelationId(record.messageId(), onlineUser.getBrokerId()))
                    .messageId(record.messageId())
                    .outboxId(outboxId)
                    .brokerId(onlineUser.getBrokerId())
                    .userIds(List.of(userId))
                    .payload(record.payload())
                    .attempt(0)
                    .firstEnqueueAt(System.currentTimeMillis())
                    .build();
            offerDispatch(task);
            messageMetricsRecorder.onDispatchCreated();
        }
    }

    private void dispatchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                MessageDispatchTask task = dispatchQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) {
                    continue;
                }
                send(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("消息分发循环异常", e);
            }
        }
    }

    private void send(MessageDispatchTask task) {
        pendingTaskMap.put(task.correlationId(), task);
        try {
            rabbitTemplate.convertAndSend(IMConstant.MQ_EXCHANGE_NAME, task.brokerId(), task.payload(),
                    new CorrelationData(task.correlationId()));
        } catch (Exception e) {
            pendingTaskMap.remove(task.correlationId());
            scheduleRetry(task, e.getMessage());
        }
    }

    private void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
        String correlationId = correlationData == null ? null : correlationData.getId();
        MessageDispatchTask task = pendingTaskMap.remove(correlationId);
        if (task == null) {
            return;
        }
        if (ack) {
            outboxRecordService.markSent(task.outboxId(), task.attempt() + 1);
            messageStatusService.markDelivered(task.messageId(), task.userIds());
            messageMetricsRecorder.onDispatchSuccess(java.time.Duration.ofMillis(System.currentTimeMillis() - task.firstEnqueueAt()));
            return;
        }
        scheduleRetry(task, cause);
    }

    private void handleReturn(ReturnedMessage returnedMessage) {
        if (returnedMessage == null || returnedMessage.getMessage() == null) {
            return;
        }
        String correlationId = Optional.ofNullable(returnedMessage.getMessage().getMessageProperties())
                .map(properties -> properties.getCorrelationId())
                .map(Object::toString)
                .orElse(null);
        MessageDispatchTask task = pendingTaskMap.remove(correlationId);
        if (task == null) {
            return;
        }
        scheduleRetry(task, returnedMessage.getReplyText());
    }

    private void scheduleRetry(MessageDispatchTask task, String reason) {
        if (task.attempt() + 1 > maxRetry) {
            messageStatusService.markFailed(task.messageId(), task.userIds(), reason);
            outboxRecordService.markDlx(task.outboxId(), task.attempt() + 1, reason);
            messageMetricsRecorder.onDispatchFailed(java.time.Duration.ofMillis(System.currentTimeMillis() - task.firstEnqueueAt()));
            for (String userId : task.userIds()) {
                offlineMessageService.store(userId, OfflineMessageRecord.builder()
                        .messageId(task.messageId())
                        .messageType(null)
                        .payload(task.payload())
                        .build());
            }
            return;
        }
        MessageDispatchTask nextTask = task.toBuilder()
                .attempt(task.attempt() + 1)
                .build();
        long delay = retryDelayMs * Math.max(1, nextTask.attempt());
        outboxRecordService.markPendingForRetry(task.outboxId(), nextTask.attempt(), System.currentTimeMillis() + delay, reason);
        messageMetricsRecorder.onDispatchRetry();
        scheduleRetryTask(nextTask, delay);
    }

    /**
     * 优先通过时间轮调度重试任务；若异常则自动回退到线程池定时调度。
     */
    private void scheduleRetryTask(MessageDispatchTask task, long delay) {
        if (retryTimeWheel == null) {
            scheduledExecutor.schedule(() -> offerDispatch(task), delay, TimeUnit.MILLISECONDS);
            return;
        }
        try {
            retryTimeWheel.schedule(() -> offerDispatch(task), delay);
        } catch (Exception e) {
            log.warn("时间轮调度失败，回退线程池调度: messageId={}, delayMs={}", task.messageId(), delay, e);
            scheduledExecutor.schedule(() -> offerDispatch(task), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void offerDispatch(MessageDispatchTask task) {
        boolean offered = dispatchQueue.offer(task);
        if (!offered) {
            scheduleRetry(task, "dispatch queue full");
        }
    }

    private IMRegisterUser getOnlineUser(String userId) {
        Object data = redisUtil.get(IMConstant.USER_CACHE_PREFIX + userId);
        if (data == null) {
            return null;
        }
        if (data instanceof IMRegisterUser user) {
            return user;
        }
        return JacksonUtils.parseObject(data, IMRegisterUser.class);
    }

    private String buildCorrelationId(String messageId, String brokerId) {
        return messageId + ":" + brokerId + ":" + IdUtils.snowflakeIdStr();
    }

    private void refreshConnectionCount() {
        int count = Optional.ofNullable(redisTemplate.keys(IMConstant.USER_CACHE_PREFIX + "*"))
                .map(Collection::size)
                .orElse(0);
        messageMetricsRecorder.setOnlineConnectionCount(count);
    }

    private RoutingPlan resolveRoutingPlan(List<String> userIds) {
        List<String> keys = userIds.stream()
                .map(userId -> IMConstant.USER_CACHE_PREFIX + userId)
                .toList();
        List<Object> onlineRecords = redisUtil.batchGet(keys);
        Map<String, List<String>> onlineBrokerUsers = new HashMap<>();
        List<String> offlineUsers = new ArrayList<>();
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            Object record = onlineRecords != null && onlineRecords.size() > i ? onlineRecords.get(i) : null;
            IMRegisterUser registerUser = toRegisterUser(record);
            if (registerUser == null || !StringUtils.hasText(registerUser.getBrokerId())) {
                offlineUsers.add(userId);
                continue;
            }
            onlineBrokerUsers.computeIfAbsent(registerUser.getBrokerId(), key -> new ArrayList<>()).add(userId);
        }
        return new RoutingPlan(onlineBrokerUsers, offlineUsers);
    }

    private IMRegisterUser toRegisterUser(Object data) {
        if (Objects.isNull(data)) {
            return null;
        }
        if (data instanceof IMRegisterUser user) {
            return user;
        }
        return JacksonUtils.parseObject(data, IMRegisterUser.class);
    }

    private record RoutingPlan(
            Map<String, List<String>> onlineBrokerUsers,
            List<String> offlineUsers
    ) {
    }
}
