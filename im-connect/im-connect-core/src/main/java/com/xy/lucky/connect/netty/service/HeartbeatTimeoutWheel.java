package com.xy.lucky.connect.netty.service;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轻量级心跳超时检测时间轮
 * <p>
 * 设计目标：
 * 1. 避免为每个连接创建独立定时任务，降低高并发场景下的调度开销
 * 2. 使用惰性失效策略，续期时不做 O(n) 删除
 * 3. 超时处理切回 Channel 的 EventLoop 执行，保证线程安全
 */
@Slf4j(topic = LogConstant.HeartBeat)
@Component
public class HeartbeatTimeoutWheel {

    private static final int WHEEL_SIZE = 512;
    private static final int WHEEL_MASK = WHEEL_SIZE - 1;
    private static final AttributeKey<Boolean> CLOSE_LISTENER_BOUND =
            AttributeKey.valueOf("im_heartbeat_timeout_wheel_close_listener_bound");

    private final AtomicLong currentTick = new AtomicLong(0);
    private final ConcurrentHashMap<String, TimeoutTask> latestTaskMap = new ConcurrentHashMap<>();
    private final List<ConcurrentLinkedQueue<TimeoutTask>> buckets = new ArrayList<>(WHEEL_SIZE);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;
    private long tickMs;
    private long timeoutTicks;

    @Autowired
    private NettyProperties nettyProperties;

    @Autowired
    private ChannelCleanupHelper cleanupHelper;

    @PostConstruct
    public void start() {
        for (int i = 0; i < WHEEL_SIZE; i++) {
            buckets.add(new ConcurrentLinkedQueue<>());
        }

        this.tickMs = resolveTickMs(nettyProperties.getHeartBeatTime());
        this.timeoutTicks = Math.max(1L, (nettyProperties.getHeartBeatTime() + tickMs - 1L) / tickMs);

        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "im-heartbeat-wheel");
            t.setDaemon(true);
            return t;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.scheduler.scheduleAtFixedRate(this::onTick, tickMs, tickMs, TimeUnit.MILLISECONDS);
        running.set(true);

        log.info("心跳时间轮已启动: tickMs={}, timeoutTicks={}, wheelSize={}", tickMs, timeoutTicks, WHEEL_SIZE);
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        latestTaskMap.clear();
        buckets.forEach(ConcurrentLinkedQueue::clear);
        log.info("心跳时间轮已关闭");
    }

    /**
     * 首次接入连接时注册超时任务
     */
    public void register(Channel channel) {
        renew(channel);
        bindCloseListener(channel);
    }

    /**
     * 收到业务消息后续期
     */
    public void renew(Channel channel) {
        if (!running.get() || channel == null || !channel.isActive()) {
            return;
        }

        long expireTick = currentTick.get() + timeoutTicks;
        TimeoutTask task = new TimeoutTask(channel, expireTick);
        latestTaskMap.put(channel.id().asLongText(), task);
        scheduleTask(task, currentTick.get());
    }

    /**
     * 连接关闭时移除索引，桶内旧任务由惰性失效自动跳过
     */
    public void unregister(Channel channel) {
        if (channel == null) {
            return;
        }
        latestTaskMap.remove(channel.id().asLongText());
    }

    private void bindCloseListener(Channel channel) {
        if (channel == null) {
            return;
        }
        Boolean bound = channel.attr(CLOSE_LISTENER_BOUND).get();
        if (Boolean.TRUE.equals(bound)) {
            return;
        }
        channel.attr(CLOSE_LISTENER_BOUND).set(Boolean.TRUE);
        channel.closeFuture().addListener(f -> unregister(channel));
    }

    private void onTick() {
        if (!running.get()) {
            return;
        }
        try {
            long tick = currentTick.incrementAndGet();
            int slot = (int) (tick & WHEEL_MASK);
            ConcurrentLinkedQueue<TimeoutTask> bucket = buckets.get(slot);

            int batch = bucket.size();
            for (int i = 0; i < batch; i++) {
                TimeoutTask task = bucket.poll();
                if (task == null) {
                    continue;
                }

                String channelId = task.channel.id().asLongText();
                TimeoutTask latest = latestTaskMap.get(channelId);
                if (latest != task) {
                    continue;
                }

                if (task.remainingRounds > 0) {
                    task.remainingRounds--;
                    bucket.offer(task);
                    continue;
                }

                if (task.expireTick > tick) {
                    scheduleTask(task, tick);
                    continue;
                }

                if (!task.channel.isActive()) {
                    latestTaskMap.remove(channelId, task);
                    continue;
                }

                if (latestTaskMap.remove(channelId, task)) {
                    fireTimeout(task.channel);
                }
            }
        } catch (Throwable t) {
            log.error("心跳时间轮执行异常: {}", t.getMessage(), t);
        }
    }

    private void scheduleTask(TimeoutTask task, long baseTick) {
        long delayTicks = Math.max(1L, task.expireTick - baseTick);
        long rounds = (delayTicks - 1L) / WHEEL_SIZE;
        int slot = (int) (task.expireTick & WHEEL_MASK);
        task.remainingRounds = rounds;
        buckets.get(slot).offer(task);
    }

    private void fireTimeout(Channel channel) {
        channel.eventLoop().execute(() -> {
            if (!channel.isActive()) {
                return;
            }
            cleanupHelper.cleanup(channel, "heartbeat_timeout_wheel", true);
        });
    }

    private long resolveTickMs(int heartbeatTimeMs) {
        if (heartbeatTimeMs <= 0) {
            return 1000L;
        }
        long adaptiveTick = heartbeatTimeMs / 8L;
        return Math.max(200L, Math.min(1000L, adaptiveTick));
    }

    private static class TimeoutTask {
        private final Channel channel;
        private final long expireTick;
        private long remainingRounds;

        private TimeoutTask(Channel channel, long expireTick) {
            this.channel = channel;
            this.expireTick = expireTick;
        }
    }
}
