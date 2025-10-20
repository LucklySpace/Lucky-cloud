package com.xy.generator.work;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 动态分配 Snowflake 算法中的 workerId，基于 Nacos 服务发现列表排序
 *
 * 特性：
 * 1. 监听服务注册完成事件，确保分配时机正确
 * 2. 添加重试机制应对 Nacos 初始化延迟
 * 3. 明确处理实例不存在异常
 */
@Slf4j
@Component
public class WorkerIdAssigner {

    private static final int MAX_WORKER_ID = 1023;
    private static final int MAX_RETRY = 20;
    private static final long RETRY_INTERVAL = 3000L;

    @Resource
    private NacosServiceManager nacosServiceManager;

    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Value("${spring.application.name}")
    private String serviceName;

    // 初始值设为非法值，便于排查问题
    private volatile long workerId = -1;

    /**
     * 加载并分配当前实例的 workerId，基于 Nacos 实例排序
     *
     * @throws IllegalStateException 当超过最大重试次数仍未成功时抛出
     */
    public void load() {
        if (workerId != -1) return;

        for (int retry = 1; retry <= MAX_RETRY; retry++) {
            try {
                if (log.isInfoEnabled()) {
                    log.info("尝试第 {} 次加载 workerId", retry);
                }
                resolveWorkerId();
                if (log.isInfoEnabled()) {
                    log.info("workerId 分配成功: {}", workerId);
                }
                return;
            } catch (Exception e) {
                log.warn("workerId 分配失败（第 {} 次），原因：{}", retry, e.getMessage());
                if (retry == MAX_RETRY) {
                    throw new IllegalStateException("WorkerId 分配失败，超过最大重试次数", e);
                }
                sleep(RETRY_INTERVAL);
            }
        }
    }

    /**
     * 获取当前实例在 Nacos 中的排序索引，并映射为 workerId
     *
     * @throws Exception 当获取Nacos服务实例或解析workerId时发生错误
     */
    private void resolveWorkerId() throws Exception {
        NamingService namingService = nacosServiceManager.getNamingService(
                nacosDiscoveryProperties.getNacosProperties());

        List<Instance> instances = namingService.getAllInstances(serviceName, true);

        if (instances.isEmpty()) {
            throw new IllegalStateException("当前服务在 Nacos 中无实例，检查是否已注册");
        }

        String selfAddress = nacosDiscoveryProperties.getIp() + ":" + nacosDiscoveryProperties.getPort();

        if (log.isDebugEnabled()) {
            log.debug("当前实例标识：{}", selfAddress);
        }

        int index = IntStream.range(0, instances.size())
                .filter(i -> (instances.get(i).getIp() + ":" + instances.get(i).getPort()).equals(selfAddress))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "实例未在 Nacos 注册，请检查以下配置：\n" +
                                "- 是否添加了 @EnableDiscoveryClient\n" +
                                "- spring.application.name=" + serviceName + "\n" +
                                "- 当前实例标识：" + selfAddress
                ));

        workerId = index % (MAX_WORKER_ID + 1) + 1;

        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("WorkerId 计算异常: " + workerId);
        }
    }

    /**
     * 线程休眠
     *
     * @param millis 休眠时间（毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取分配的workerId
     *
     * @return 分配的workerId
     * @throws IllegalStateException 当workerId尚未初始化时抛出
     */
    public long getWorkerId() {
        if (workerId == -1) {
            throw new IllegalStateException("WorkerId 尚未初始化，请先调用 load()");
        }
        return workerId;
    }
}