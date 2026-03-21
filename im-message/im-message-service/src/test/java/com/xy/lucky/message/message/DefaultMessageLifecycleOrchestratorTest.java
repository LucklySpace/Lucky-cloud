package com.xy.lucky.message.message;

import com.xy.lucky.message.message.offline.OfflineMessageRecord;
import com.xy.lucky.message.message.offline.OfflineMessageService;
import com.xy.lucky.message.message.outbox.OutboxRecordService;
import com.xy.lucky.message.message.status.MessageStatusService;
import com.xy.lucky.message.message.monitor.MessageMetricsRecorder;
import com.xy.lucky.message.utils.RedisUtil;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.model.IMRegisterUser;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
        orchestrator.acknowledge("m1", "u1");

        verify(messageStatusService).acknowledge("m1", "u1");
        verify(outboxRecordService).markDeliveredByMessageId("m1");
    }
}
