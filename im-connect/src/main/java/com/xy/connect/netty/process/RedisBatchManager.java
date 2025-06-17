package com.xy.connect.netty.process;

import cn.hutool.core.util.ObjectUtil;
import com.xy.connect.redis.RedisTemplate;
import com.xy.imcore.model.IMRegisterUser;
import com.xy.imcore.utils.StringUtils;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.PostConstruct;
import com.xy.spring.annotations.core.Value;
import com.xy.spring.core.DisposableBean;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.xy.imcore.constants.IMConstant.IM_USER_PREFIX;


@Slf4j
@Component
public class RedisBatchManager implements DisposableBean {

    /**
     * 每批最大处理数量，防止单次处理过多导致 Redis 压力或阻塞
     */
    private static final int MAX_BATCH_SIZE = 1000;

    /**
     * 最大线程数，用于动态分批时最多并发提交多少批任务
     */
    private static final int MAX_THREAD_POOL = 2;

    /**
     * 三类处理任务的类型常量，避免使用中文字符串判断
     */
    private static final String TASK_ADD = "ADD";
    private static final String TASK_EXPIRE = "EXPIRE";
    private static final String TASK_DELETE = "DELETE";
    /**
     * 等待新增用户连接的队列（包含 userId、token、机器信息等）
     */
    private final BlockingQueue<IMRegisterUser> addQueue = new LinkedBlockingQueue<>();
    /**
     * 等待续期的用户 userId 队列（接收到心跳）
     */
    private final BlockingQueue<String> expireQueue = new LinkedBlockingQueue<>();
    /**
     * 等待删除的用户 userId 队列（下线或断开连接）
     */
    private final BlockingQueue<String> deleteQueue = new LinkedBlockingQueue<>();
    /**
     * 三个独立线程池，分别处理 新增 / 续期 / 删除 操作，确保互不阻塞
     */
    private final ScheduledExecutorService addScheduler = Executors.newScheduledThreadPool(MAX_THREAD_POOL);
    private final ScheduledExecutorService expireScheduler = Executors.newScheduledThreadPool(MAX_THREAD_POOL);
    private final ScheduledExecutorService deleteScheduler = Executors.newScheduledThreadPool(MAX_THREAD_POOL);
    /**
     * 心跳时间（秒），从配置中读取
     * 实际 Redis key 的 TTL 设置为 heartBeatTime * 2
     */
    @Value("netty.config.heartBeatTime")
    private Integer heartBeatTime;
    /**
     * Redis 操作工具类，封装了批量 setnx / expire / delete 方法
     */
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 初始化方法，在容器启动后自动执行
     * 为三个队列启动动态处理任务（每 100ms 扫描一次队列）
     */
    @PostConstruct
    public void init() {
        // 启动新增用户处理任务
        startDynamicScheduler(addQueue, this::processAddQueue, TASK_ADD);

        // 启动续期（心跳）处理任务
        startDynamicScheduler(expireQueue, this::processExpireQueue, TASK_EXPIRE);

        // 启动删除用户处理任务
        startDynamicScheduler(deleteQueue, this::processDeleteQueue, TASK_DELETE);

        // JVM关闭时优雅退出
        Runtime.getRuntime().addShutdownHook(new Thread(this::destroy));
    }

    /**
     * 启动动态批处理调度器（每个类型一个线程池）
     *
     * @param queue 等待处理的队列
     * @param task  处理逻辑
     * @param type  任务类型（ADD / EXPIRE / DELETE）
     * @param <T>   队列中数据的类型
     */
    private <T> void startDynamicScheduler(BlockingQueue<T> queue, Runnable task, String type) {
        // 根据类型选择对应的调度线程池
        ScheduledExecutorService scheduler = switch (type) {
            case TASK_ADD -> addScheduler;
            case TASK_EXPIRE -> expireScheduler;
            case TASK_DELETE -> deleteScheduler;
            default -> throw new IllegalArgumentException("未知任务类型: " + type);
        };

        // 启动定时调度任务：每 100ms 检查一次队列长度并按需分批处理
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int size = queue.size();
                if (size == 0) return;

                // 计算处理所需批次数（向上取整），控制最多并发 MAX_THREAD_POOL 批次
                int loop = (size + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;
                for (int i = 0; i < Math.min(loop, MAX_THREAD_POOL); i++) {
                    scheduler.submit(task); // 并发提交分批任务
                }
            } catch (Exception e) {
                log.error("任务 [{}] 执行异常", type, e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 处理新增用户连接：批量保存用户信息并设置过期时间（setnx 方式）
     */
    private void processAddQueue() {
        List<IMRegisterUser> batch = new ArrayList<>(MAX_BATCH_SIZE);
        addQueue.drainTo(batch, MAX_BATCH_SIZE);
        if (batch.isEmpty()) return;

        // 将用户信息转换为 <userId, 用户对象>
        Map<String, Object> map = batch.stream()
                .collect(Collectors.toMap(IMRegisterUser::getUserId, each -> each, (v1, v2) -> v1));

        // 批量 setnx，仅在 key 不存在时写入
        redisTemplate.setnxBatch(IM_USER_PREFIX, map, heartBeatTime * 2);
        log.debug("批量新增用户信息，数量：{}", batch.size());
    }

    /**
     * 处理用户心跳续期：批量延长过期时间
     */
    private void processExpireQueue() {
        List<String> batch = new ArrayList<>(MAX_BATCH_SIZE);
        expireQueue.drainTo(batch, MAX_BATCH_SIZE);
        if (batch.isEmpty()) return;

        // 批量续期（设置新的过期时间）
        redisTemplate.expireBatch(IM_USER_PREFIX, batch, heartBeatTime * 2);
        log.debug("批量续期用户 TTL，数量：{}", batch.size());
    }

    /**
     * 处理用户下线：批量删除 Redis 中的 key
     */
    private void processDeleteQueue() {
        List<String> batch = new ArrayList<>(MAX_BATCH_SIZE);
        deleteQueue.drainTo(batch, MAX_BATCH_SIZE);
        if (batch.isEmpty()) return;

        // 批量删除用户连接记录
        redisTemplate.deleteBatch(IM_USER_PREFIX, batch);
        log.debug("批量删除用户信息，数量：{}", batch.size());
    }

    // =================== 外部调用入口 ===================

    /**
     * 新增用户连接（仅在用户首次上线时调用）
     *
     * @param user 用户连接信息（包含 userId、token、机器码）
     */
    public void onUserAdd(IMRegisterUser user) {
        if (ObjectUtil.isNotEmpty(user)) addQueue.offer(user);
    }

    /**
     * 用户心跳续期（用户每隔 X 秒发送心跳包时调用）
     *
     * @param userId 用户 ID
     */
    public void onUserHeartbeat(String userId) {
        if (StringUtils.hasText(userId)) expireQueue.offer(userId);
    }

    /**
     * 用户断链或主动离线（由连接断开事件调用）
     *
     * @param userId 用户 ID
     */
    public void onUserDelete(String userId) {
        if (StringUtils.hasText(userId)) deleteQueue.offer(userId);
    }

    /**
     * Bean 销毁时自动关闭线程池，防止资源泄漏
     */
    @Override
    public void destroy() {
        addScheduler.shutdownNow();
        expireScheduler.shutdownNow();
        deleteScheduler.shutdownNow();
    }
}
