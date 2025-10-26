package com.xy.connect.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.connect.channel.UserChannelMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.utils.IPAddressUtil;
import com.xy.core.constants.NacosMetadataConstants;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Nacos 注册模板（线程安全，高可用）
 */
@Slf4j(topic = LogConstant.Nacos)
@Component
public class NacosTemplate {

    // 连接数上报间隔（可改为从配置读取）
    private static final long REPORT_INTERVAL_SECONDS = 10L;
    // 重试注册的初始延迟（ms）与上限（ms）
    private static final long RETRY_BASE_DELAY_MS = 2000L;
    private static final long RETRY_MAX_DELAY_MS = 60_000L;
    // scheduler：单线程, 定时上报、重试都在这里调度
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nacos-template-scheduler");
        t.setDaemon(true);
        return t;
    });
    // NamingService 单例引用（延迟创建）
    private final AtomicReference<NamingService> namingServiceRef = new AtomicReference<>(null);
    // 管理已注册的实例：port -> Instance
    private final ConcurrentMap<Integer, Instance> instances = new ConcurrentHashMap<>();
    // Nacos 服务地址配置（通过注入）
    @Value("${nacos.config.address}")
    private String serverAddr;
    @Value("${nacos.config.name}")
    private String serviceName;
    @Value("${nacos.config.version:}")
    private String version;
    @Value("${brokerId:}")
    private String brokerId;
    @Value("${netty.config.protocol:proto}")
    protected String protocolType;
    @Value("${netty.config.path:/im}")
    protected String wsPath;
    @Value("${nacos.config.group}")
    private String groupName;

    // 上报任务句柄（用于停止）
    private volatile ScheduledFuture<?> reporterFuture;
    // 最多尝试创建 NamingService 多少次？这里我们采用无限重试，直到成功或 shutdown。
    private volatile boolean shutdown = false;

    @Autowired
    private UserChannelMap userChannelMap;

    /**
     * 注册服务到 Nacos（为指定端口注册一个实例）
     * - 非阻塞：发生错误会触发重试（在后台尝试）
     *
     * @param port 监听端口（必需）
     * @return true 表示已成功注册；false 表示当前尚未注册（会在后台重试）
     */
    public boolean registerNacos(Integer port) {
        Objects.requireNonNull(port, "port");

        if (shutdown) {
            log.warn("NacosTemplate 已 shutdown，忽略 registerNacos({}) 请求", port);
            return false;
        }

        // 如果已经注册过此端口，直接返回 true
        if (instances.containsKey(port)) {
            log.debug("port={} 已注册，跳过重复注册", port);
            return true;
        }

        // 构建 Instance
        String ip = safeGetLocalIp();
        if (ip == null || ip.isEmpty()) {
            log.warn("未能获取本机 IP，使用 localhost 作为回退");
            ip = "127.0.0.1";
        }

        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(port);
        instance.setServiceName(serviceName);
        instance.setEnabled(true);
        instance.setHealthy(true);
        instance.setWeight(1.0);
        // metadata
        Map<String, String> meta = instance.getMetadata();
        meta.put(NacosMetadataConstants.BROKER_ID, brokerId == null ? "" : brokerId);
        meta.put(NacosMetadataConstants.VERSION, version == null ? "" : version);
        meta.put(NacosMetadataConstants.WS_PATH, wsPath);

        // # TODO 待完善
        meta.put(NacosMetadataConstants.PROTOCOLS, "[\"proto\"]"); // json array format
        meta.put(NacosMetadataConstants.CONNECTION, "0");
        meta.put(NacosMetadataConstants.REGION, "cn-shanghai");
        meta.put(NacosMetadataConstants.PRIORITY, "1");

        // 把实例放到 map（但尚未注册成功）
        instances.put(port, instance);

        // 尝试立即注册（若失败会在后台按指数回退重试）
        scheduler.execute(() -> tryRegisterWithRetry(port, instance, 0L));

        // 确保 reporter 在首次注册请求后启动
        startReporterIfNeeded();

        return false; // 注册是否成功由 tryRegisterWithRetry 最终决定并会被日志记录
    }

    public boolean batchRegisterNacos(List<Integer> portList) {
        Objects.requireNonNull(portList, "portList");

        List<Instance> instanceList = new ArrayList<>();
        for (Integer port : portList) {
            // 为每一个端口创建一个 Instance
            if (instances.containsKey(port)) {
                log.debug("port={} 已注册，跳过重复注册", port);
                return true;
            }

            // 开始构建每一个 Instance
            String ip = safeGetLocalIp();
            if (ip == null || ip.isEmpty()) {
                log.warn("未能获取本机 IP，使用 localhost 作为回退");
                ip = "127.0.0.1";
            }

            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setServiceName(serviceName);
            instance.setEnabled(true);
            instance.setHealthy(true);
            instance.setWeight(1.0);
            // metadata
            Map<String, String> meta = instance.getMetadata();
            meta.put(NacosMetadataConstants.BROKER_ID, brokerId == null ? "" : brokerId);
            meta.put(NacosMetadataConstants.VERSION, version == null ? "" : version);
            meta.put(NacosMetadataConstants.WS_PATH, wsPath);

            // # TODO 待完善
            meta.put(NacosMetadataConstants.PROTOCOLS, "[\"proto\"]"); // json array format
            meta.put(NacosMetadataConstants.CONNECTION, "0");
            meta.put(NacosMetadataConstants.REGION, "cn-shanghai");
            meta.put(NacosMetadataConstants.PRIORITY, "1");

            // 把实例放到 map（但尚未注册成功）
            instances.put(port, instance);
            instanceList.add(instance);
        }

        NamingService ns = ensureNamingService();
        if (ns == null) {
            log.warn("NamingService is null");
            return false;
        }

        try {
            ns.batchRegisterInstance(serviceName, groupName, instanceList);
        } catch (NacosException e) {
            log.warn("批量注册Instance失败");
        }

        return false;
    }

    /**
     * 尝试注册并在失败时重试（指数回退）
     *
     * @param port           端口
     * @param instance       待注册的 Instance
     * @param attemptDelayMs 当前尝试延迟（初始 0 表示立即尝试）
     */
    private void tryRegisterWithRetry(int port, Instance instance, long attemptDelayMs) {
        if (shutdown) return;

        Runnable task = () -> {
            if (shutdown) return;
            try {
                NamingService ns = ensureNamingService();
                if (ns == null) {
                    // 无法创建 namingService，安排重试
                    scheduleRetry(port, instance, RETRY_BASE_DELAY_MS);
                    return;
                }
                ns.registerInstance(serviceName, groupName, instance);
                log.info("Service registered to Nacos successfully: {}:{} (port={})", instance.getIp(), serviceName, port);
            } catch (NacosException e) {
                log.warn("Nacos register failed (port={}), will retry: {}", port, e.getMessage());
                scheduleRetry(port, instance, Math.min(RETRY_MAX_DELAY_MS, attemptDelayMs == 0 ? RETRY_BASE_DELAY_MS : Math.max(RETRY_BASE_DELAY_MS, attemptDelayMs * 2)));
            } catch (Exception e) {
                log.error("Unexpected exception while registering to Nacos (port={})", port, e);
                scheduleRetry(port, instance, RETRY_BASE_DELAY_MS);
            }
        };

        if (attemptDelayMs > 0) {
            scheduler.schedule(task, attemptDelayMs, TimeUnit.MILLISECONDS);
        } else {
            scheduler.execute(task);
        }
    }

    private void scheduleRetry(int port, Instance instance, long delayMs) {
        if (shutdown) return;
        long d = 0;
        d = Math.max(RETRY_BASE_DELAY_MS, delayMs);
        log.debug("Scheduling Nacos register retry for port={} after {} ms", port, d);
        long finalD = d;
        scheduler.schedule(() -> tryRegisterWithRetry(port, instance, finalD), d, TimeUnit.MILLISECONDS);
    }

    /**
     * 注销某个端口对应的实例（如果已注册）
     *
     * @param port 端口
     * @return true 表示已注销或未注册（幂等）
     */
    public boolean deregisterNacos(int port) {
        Objects.requireNonNull(port, "port");
        Instance inst = instances.remove(port);
        if (inst == null) {
            log.debug("deregisterNacos: port={} 未注册，忽略", port);
            return true;
        }

        try {
            NamingService ns = namingServiceRef.get();
            if (ns != null) {
                ns.deregisterInstance(serviceName, inst.getIp(), inst.getPort());
                log.info("Deregistered instance from Nacos: {}:{}", inst.getIp(), inst.getPort());
            } else {
                log.warn("NamingService not initialized when deregistering port={}", port);
            }
        } catch (Exception e) {
            log.warn("Failed to deregister instance (port={}), will ignore: {}", port, e.getMessage());
        }
        return true;
    }

    /**
     * 手动更新某个实例的 connection metadata 并向 Nacos 注册（更新）
     *
     * @param port  端口
     * @param count 连接数
     * @return true 成功
     */
    public boolean updateConnectionCount(int port, int count) {
        Instance inst = instances.get(port);
        if (inst == null) {
            log.warn("updateConnectionCount: no instance registered for port={}", port);
            return false;
        }
        inst.getMetadata().put("connection", String.valueOf(count));
        try {
            NamingService ns = namingServiceRef.get();
            if (ns != null) {
                ns.registerInstance(serviceName, inst); // registerInstance 会更新已有实例的 metadata
                return true;
            } else {
                log.debug("NamingService not ready, updateConnectionCount will be retried by reporter");
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed to update connection count to Nacos for port={}: {}", port, e.getMessage());
            return false;
        }
    }

    /**
     * 启动周期性上报任务（如果尚未启动）
     */
    private synchronized void startReporterIfNeeded() {
        if (reporterFuture == null || reporterFuture.isCancelled() || reporterFuture.isDone()) {
            reporterFuture = scheduler.scheduleAtFixedRate(this::reportAllConnectionCounts, 5, REPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
            log.info("Nacos connection reporter started, interval={}s", REPORT_INTERVAL_SECONDS);
        }
    }

    /**
     * 上报所有已注册 instance 的连接数（会遍历 instances map 并逐个更新 metadata）
     * 注意: 这里演示使用全局连接数；根据你的 UserChannelCtxMap 能力，可以改为按端口统计。
     */
    private void reportAllConnectionCounts() {
        if (shutdown) return;
        try {
            NamingService ns = namingServiceRef.get();
            if (ns == null) {
                // 不要频繁创建 NamingService；reporter 不直接初始化，但可以调用 ensureNamingService 如果希望
                ensureNamingService(); // 尝试懒初始化
                ns = namingServiceRef.get();
                if (ns == null) {
                    log.debug("NamingService not available yet, skip reporting");
                    return;
                }
            }

            // 当前所有连接（全局示例），如果你能按端口统计，请替换这里
            int total = safeGetCurrentConnectionCount();

            for (Map.Entry<Integer, Instance> e : instances.entrySet()) {
                Instance inst = e.getValue();
                if (inst == null) continue;
                inst.getMetadata().put("connection", String.valueOf(total));
                try {
                    ns.registerInstance(serviceName, inst); // 注册会更新 metadata
                } catch (Exception regEx) {
                    log.debug("Failed to update connection metadata for port={} : {}", e.getKey(), regEx.getMessage());
                    // 不做立即重试，下一次 report 会再尝试
                }
            }
        } catch (Throwable t) {
            log.warn("Unexpected error in Nacos reporter", t);
        }
    }

    /**
     * 安全获取当前连接数（封装 UserChannelCtxMap），避免异常影响主流程
     */
    private int safeGetCurrentConnectionCount() {
        try {
            return userChannelMap.getOnlineUserCount();
        } catch (Throwable t) {
            log.debug("Failed to get connection count, fallback to 0", t);
            return 0;
        }
    }

    /**
     * 延迟初始化 NamingService（线程安全）
     *
     * @return NamingService 实例或 null（若创建失败）
     */
    private synchronized NamingService ensureNamingService() {
        if (shutdown) return namingServiceRef.get();
        NamingService existing = namingServiceRef.get();
        if (existing != null) return existing;

        // 校验 server 地址
        if (serverAddr == null) {
            log.error("Nacos server config is incomplete: serverAddr={}", serverAddr);
            return null;
        }

        try {
            NamingService ns = NamingFactory.createNamingService(serverAddr);
            namingServiceRef.set(ns);
            log.info("Initialized NamingService to Nacos at {}", serverAddr);
            return ns;
        } catch (NacosException e) {
            log.warn("Failed to create NamingService at {}: {}. Will retry later.", serverAddr, e.getMessage());
            // 在后台安排重试（避免阻塞调用线程）
            scheduler.schedule(() -> {
                if (!shutdown) {
                    ensureNamingService();
                }
            }, RETRY_BASE_DELAY_MS, TimeUnit.MILLISECONDS);
            return null;
        } catch (Throwable t) {
            log.error("Unexpected error creating NamingService: {}", t.getMessage(), t);
            return null;
        }
    }

    /**
     * 关闭 NacosTemplate：停止上报、注销所有实例并关闭 NamingService
     */
    public synchronized void shutdown() {
        if (shutdown) return;
        shutdown = true;

        // 取消 reporter
        if (reporterFuture != null) {
            reporterFuture.cancel(false);
        }

        // 反注册所有实例
        for (Map.Entry<Integer, Instance> e : instances.entrySet()) {
            try {
                NamingService ns = namingServiceRef.get();
                if (ns != null) {
                    Instance inst = e.getValue();
                    ns.deregisterInstance(serviceName, inst.getIp(), inst.getPort());
                    log.info("Deregistered instance on shutdown: {}:{}", inst.getIp(), inst.getPort());
                }
            } catch (Exception ex) {
                log.warn("Failed to deregister instance during shutdown: port={}", e.getKey(), ex);
            }
        }
        instances.clear();

        // 关闭 NamingService（如果 client 提供 shutdown）
        try {
            NamingService ns = namingServiceRef.getAndSet(null);
            if (ns != null) {
                try {
                    // 如果你的 Nacos client 提供 close/shutdown API，调用它（某些版本方法名为 shutdown / stop）
                    ns.shutDown(); // 如果该方法在你的客户端版本不存在，请替换为正确的方法或去掉
                } catch (Throwable tt) {
                    // 有些 nacos client 版本没有 shutDown() 方法，忽略
                    log.debug("NamingService shutdown call threw", tt);
                }
            }
        } finally {
            // 关闭调度器
            try {
                scheduler.shutdownNow();
            } catch (Throwable t) {
                log.debug("scheduler shutdown error", t);
            }
            log.info("NacosTemplate shutdown completed");
        }
    }

    // ---------- 辅助方法 ----------

    private boolean validateConfig() {
        if (serviceName == null || serviceName.isEmpty()) {
            log.error("nacos.config.name (serviceName) is not configured");
            return false;
        }
        return true;
    }

    private String safeGetLocalIp() {
        try {
            String ip = IPAddressUtil.getLocalIp4Address();
            if (ip == null || ip.isEmpty()) {
                InetAddress addr = InetAddress.getLocalHost();
                return addr.getHostAddress();
            }
            return ip;
        } catch (Throwable t) {
            log.warn("Failed to get local IP, fallback to 127.0.0.1", t);
            return "127.0.0.1";
        }
    }
}

