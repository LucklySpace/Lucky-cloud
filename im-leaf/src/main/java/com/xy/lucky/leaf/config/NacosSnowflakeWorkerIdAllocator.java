package com.xy.lucky.leaf.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于 Nacos 的 Snowflake 机器 ID 分配器。
 * <p>
 * 核心特性：
 * - 在 Spring 容器启动时 (@PostConstruct) 自动分配唯一 workerId，支持重试机制以处理并发冲突，确保高可用。
 * - 在容器销毁时 (@PreDestroy) 自动回收 workerId，支持重试以避免遗留 ID。
 * - 使用 Nacos 配置服务存储已用 ID 列表，实现分布式协调。
 * - 假设 NACOS_DATA_ID 专用于存储 snowflake.worker.used.ids 配置项。
 * - 支持最多 1024 台机器（workerId 范围 0-1023）。
 * - 高性能：分配和回收仅在生命周期钩子中执行，非热路径；重试机制快速收敛。
 * - 高并发：通过读取-修改-发布-验证循环处理并发更新冲突。
 * - 高可用：重试机制容忍临时 Nacos 故障或网络抖动；日志记录冲突和错误。
 */
@Slf4j
@Component
public class NacosSnowflakeWorkerIdAllocator {

    // Nacos 配置项常量，与 application.yml 保持一致
    private static final String NACOS_DATA_ID = "snowflake-worker-id-config";
    private static final String NACOS_GROUP = "DEFAULT_GROUP";
    private static final String USED_IDS_KEY = "snowflake.worker.used.ids";

    // 最大 workerId 值（10 位，支持 0-1023）
    private static final long MAX_WORKER_ID = 1023L;

    // 重试参数：最大尝试次数和重试间隔（ms）
    private static final int MAX_RETRIES = 10;
    private static final long RETRY_DELAY_MS = 200L;
    // Nacos 配置服务（Spring 自动注入）
    private final ConfigService configService;
    // 分配到的 workerId（供 Snowflake 使用）
    @Getter
    private long workerId = -1L; // 初始无效值

    @Autowired
    public NacosSnowflakeWorkerIdAllocator(NacosConfigManager nacosConfigManager) {
        this.configService = nacosConfigManager.getConfigService();
    }

    /**
     * 启动时分配 workerId：读取已用列表，找到可用 ID，更新配置，并验证以处理并发。
     *
     * @throws NacosException   如果 Nacos 操作失败
     * @throws RuntimeException 如果重试后仍无法分配
     */
    @PostConstruct
    public void allocateWorkerId() throws NacosException {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {


            // 读取当前配置
            String config = configService.getConfig(NACOS_DATA_ID, NACOS_GROUP, 5000L);

            // 解析已用 ID 列表
            List<Long> usedIds = parseUsedIds(config);

            // 找到第一个可用 ID
            Long newId = findAvailableId(usedIds);
            if (newId == null) {
                throw new RuntimeException("No available workerId (max 1024 machines supported)");
            }

            // 创建新列表并排序
            List<Long> newUsedIds = new ArrayList<>(usedIds);
            newUsedIds.add(newId);
            newUsedIds.sort(Long::compareTo);

            // 构建新配置字符串
            String newUsedIdsStr = newUsedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            String newConfig = buildConfig(newUsedIdsStr);

            // 发布更新
            configService.publishConfig(NACOS_DATA_ID, NACOS_GROUP, newConfig);

            // 验证：重新读取并检查是否包含新 ID
            String verifyConfig = configService.getConfig(NACOS_DATA_ID, NACOS_GROUP, 5000L);
            List<Long> verifyUsedIds = parseUsedIds(verifyConfig);
            if (verifyUsedIds.contains(newId)) {
                this.workerId = newId;
                log.info("Allocated workerId: {} (used IDs: {})", workerId, newUsedIdsStr);
                return;
            }

            log.warn("Allocation conflict detected (attempt {}/{}), retrying...", attempt + 1, MAX_RETRIES);
            sleepQuietly(RETRY_DELAY_MS);
        }
        throw new RuntimeException("Failed to allocate workerId after " + MAX_RETRIES + " retries");
    }

    /**
     * 销毁时回收 workerId：读取列表，移除 ID，更新配置，并验证。
     *
     * @throws NacosException 如果 Nacos 操作失败（日志记录但不抛出，以避免影响 shutdown）
     */
    @PreDestroy
    public void releaseWorkerId() {
        if (workerId == -1L) {
            return; // 未分配，无需回收
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                // 读取当前配置
                String config = configService.getConfig(NACOS_DATA_ID, NACOS_GROUP, 5000L);

                // 解析已用 ID 列表
                List<Long> usedIds = parseUsedIds(config);
                if (!usedIds.contains(workerId)) {
                    log.info("workerId {} already released", workerId);
                    return;
                }

                // 创建新列表
                List<Long> newUsedIds = new ArrayList<>(usedIds);
                newUsedIds.remove(workerId);
                newUsedIds.sort(Long::compareTo);

                // 构建新配置字符串（空列表时保持键为空值）
                String newUsedIdsStr = newUsedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                String newConfig = buildConfig(newUsedIdsStr);

                // 发布更新
                configService.publishConfig(NACOS_DATA_ID, NACOS_GROUP, newConfig);

                // 验证：重新读取并检查是否移除
                String verifyConfig = configService.getConfig(NACOS_DATA_ID, NACOS_GROUP, 5000L);
                List<Long> verifyUsedIds = parseUsedIds(verifyConfig);
                if (!verifyUsedIds.contains(workerId)) {
                    log.info("Released workerId: {} (remaining used IDs: {})", workerId, newUsedIdsStr);
                    return;
                }

                log.warn("Release conflict detected (attempt {}/{}), retrying...", attempt + 1, MAX_RETRIES);
                sleepQuietly(RETRY_DELAY_MS);
            } catch (NacosException e) {
                log.error("Failed to release workerId {} (attempt {}/{})", workerId, attempt + 1, MAX_RETRIES, e);
                sleepQuietly(RETRY_DELAY_MS);
            }
        }
        log.error("Failed to release workerId {} after {} retries", workerId, MAX_RETRIES);
    }

    /**
     * 解析配置字符串中的已用 ID 列表。
     *
     * @param config Nacos 配置内容
     * @return 已用 ID 列表（空列表如果无配置或键不存在）
     */
    private List<Long> parseUsedIds(String config) {
        if (config == null || config.isEmpty() || !config.contains(USED_IDS_KEY)) {
            return new ArrayList<>();
        }
        // 分割键值（支持空格），移除多余空白
        String[] parts = config.split(USED_IDS_KEY + "\\s*:\\s*", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            return new ArrayList<>();
        }
        String idsStr = parts[1].trim().replaceAll("\\s+", "");
        return Stream.of(idsStr.split(","))
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * 在已用列表中找到第一个可用 ID。
     *
     * @param usedIds 已用 ID 列表
     * @return 可用 ID 或 null（如果全部分配）
     */
    private Long findAvailableId(List<Long> usedIds) {
        for (long i = 0; i <= MAX_WORKER_ID; i++) {
            if (!usedIds.contains(i)) {
                return i;
            }
        }
        return null;
    }

    /**
     * 构建配置字符串。
     *
     * @param usedIdsStr 已用 ID 字符串（逗号分隔）
     * @return 格式化的配置内容
     */
    private String buildConfig(String usedIdsStr) {
        return USED_IDS_KEY + ": " + usedIdsStr;
    }

    /**
     * 静默睡眠（忽略中断）。
     *
     * @param millis 睡眠毫秒
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}