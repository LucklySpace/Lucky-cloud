package com.xy.lucky.message.message;

import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.message.message.monitor.MessageMetricsRecorder;
import com.xy.lucky.message.message.offline.OfflineMessageRecord;
import com.xy.lucky.message.message.offline.OfflineMessageService;
import com.xy.lucky.message.message.dispatch.MessageDispatchTask;
import com.xy.lucky.message.message.outbox.OutboxRecordService;
import com.xy.lucky.message.message.status.MessageStatusService;
import com.xy.lucky.message.utils.RedisUtil;
import com.xy.lucky.mq.rabbit.core.RabbitTemplateFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMessageLifecycleOrchestratorTest {

    @Mock
    private RedisUtil redisUtil;
    @Mock
    private RabbitTemplateFactory rabbitTemplateFactory;
    @Mock
    private MessageStatusService messageStatusService;
    @Mock
    private OfflineMessageService offlineMessageService;
    @Mock
    private OutboxRecordService outboxRecordService;
    @Mock
    private MessageMetricsRecorder messageMetricsRecorder;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private DefaultMessageLifecycleOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new DefaultMessageLifecycleOrchestrator(
                redisUtil,
                rabbitTemplateFactory,
                messageStatusService,
                offlineMessageService,
                outboxRecordService,
                messageMetricsRecorder,
                redisTemplate
        );
        ReflectionTestUtils.setField(orchestrator, "messagePushExecutor", Executors.newSingleThreadExecutor());
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        ReflectionTestUtils.setField(orchestrator, "scheduledExecutor", scheduledExecutor);
    }

    @Test
    void dispatchShouldStoreOfflineMessageWhenUserNotOnline() {
        IMRegisterUser onlineUser = new IMRegisterUser().setUserId("u1").setBrokerId("broker-1");
        when(redisUtil.batchGet(any())).thenReturn(List.of(onlineUser, null));

        orchestrator.dispatch(1, Map.of("k", "v"), List.of("u1", "u2"), "m1");

        verify(messageStatusService).markPending(eq("m1"), anyCollection());
        verify(offlineMessageService).store(eq("u2"), any(OfflineMessageRecord.class));
        BlockingQueue<?> queue = (BlockingQueue<?>) ReflectionTestUtils.getField(orchestrator, "dispatchQueue");
        assertThat(queue).isNotNull();
        assertThat(queue).hasSize(1);
    }

    @Test
    void replayOfflineMessagesShouldEnqueueReplayTasks() {
        IMRegisterUser onlineUser = new IMRegisterUser().setUserId("u1").setBrokerId("broker-1");
        when(redisUtil.get(IMConstant.USER_CACHE_PREFIX + "u1")).thenReturn(onlineUser);
        when(offlineMessageService.pull("u1", 200)).thenReturn(List.of(
                new OfflineMessageRecord("m1", 1, "{}"),
                new OfflineMessageRecord("m2", 1, "{}")
        ));

        orchestrator.replayOfflineMessages("u1");

        BlockingQueue<?> queue = (BlockingQueue<?>) ReflectionTestUtils.getField(orchestrator, "dispatchQueue");
        assertThat(queue).isNotNull();
        assertThat(queue).hasSize(2);
    }

    @Test
    void acknowledgeShouldDelegateToStatusService() {
        when(redisUtil.setIfAbsent("im:outbox:delivered:m1", "1", 24 * 3600L)).thenReturn(true);
        when(outboxRecordService.markDeliveredByMessageId("m1")).thenReturn(true);

        orchestrator.acknowledge("m1", "u1");

        verify(messageStatusService).acknowledge("m1", "u1");
        verify(outboxRecordService).markDeliveredByMessageId("m1");
    }

    @Test
    void checkPendingTimeoutTasksShouldHandleTimeoutTasksWithoutIteratorRemove() {
        ReflectionTestUtils.setField(orchestrator, "confirmTimeoutMs", 1000L);
        ReflectionTestUtils.setField(orchestrator, "maxRetry", 3);
        ReflectionTestUtils.setField(orchestrator, "retryDelayMs", 100L);

        MessageDispatchTask task = MessageDispatchTask.builder()
                .correlationId("c1")
                .messageId("m1")
                .outboxId(1L)
                .brokerId("broker-1")
                .userIds(List.of("u1"))
                .payload("{}")
                .attempt(0)
                .firstEnqueueAt(System.currentTimeMillis() - 5000L)
                .build();

        ConcurrentMap<String, MessageDispatchTask> pendingTaskMap =
                (ConcurrentMap<String, MessageDispatchTask>) ReflectionTestUtils.getField(orchestrator, "pendingTaskMap");
        ConcurrentMap<String, Long> pendingTaskStartMap =
                (ConcurrentMap<String, Long>) ReflectionTestUtils.getField(orchestrator, "pendingTaskStartMap");
        assertThat(pendingTaskMap).isNotNull();
        assertThat(pendingTaskStartMap).isNotNull();
        pendingTaskMap.put("c1", task);
        pendingTaskStartMap.put("c1", System.currentTimeMillis() - 5000L);

        ReflectionTestUtils.invokeMethod(orchestrator, "checkPendingTimeoutTasks");

        assertThat(pendingTaskMap).doesNotContainKey("c1");
        assertThat(pendingTaskStartMap).doesNotContainKey("c1");
        verify(outboxRecordService).markPendingForRetry(eq(1L), eq(1), anyLong(), eq("broker confirm timeout"));
    }
}
