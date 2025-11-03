package com.xy.connect.limiter;

import com.xy.connect.config.LogConstant;
import com.xy.connect.monitoring.MonitoringService;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接限流器
 * - 限制单个用户的最大连接数
 * - 限制全局最大连接数
 * - 提供连接计数和监控
 */
@Slf4j(topic = LogConstant.Limiter)
@Component
public class ConnectionLimiter {

    // 默认单用户最大连接数
    private static final int DEFAULT_MAX_CONNECTIONS_PER_USER = 5;
    
    // 默认全局最大连接数
    private static final int DEFAULT_MAX_GLOBAL_CONNECTIONS = 10000;
    
    // 单用户最大连接数
    private int maxConnectionsPerUser = DEFAULT_MAX_CONNECTIONS_PER_USER;
    
    // 全局最大连接数
    private int maxGlobalConnections = DEFAULT_MAX_GLOBAL_CONNECTIONS;
    
    // 用户连接计数
    private final Map<String, AtomicInteger> userConnectionCounts = new ConcurrentHashMap<>();
    
    // 全局连接计数
    private final AtomicInteger globalConnectionCount = new AtomicInteger(0);
    
    @Autowired(required = false)
    private MonitoringService monitoringService;
    
    /**
     * 检查用户是否可以建立新连接
     * @param userId 用户ID
     * @return 是否允许连接
     */
    public boolean canConnect(String userId) {
        // 检查全局连接数
        if (globalConnectionCount.get() >= maxGlobalConnections) {
            log.warn("全局连接数已达上限: {}", maxGlobalConnections);
            if (monitoringService != null) {
                monitoringService.incrementCounter("limiter.global_limit_reached");
            }
            return false;
        }
        
        // 检查用户连接数
        AtomicInteger count = userConnectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        if (count.get() >= maxConnectionsPerUser) {
            log.warn("用户连接数已达上限: userId={}, limit={}", userId, maxConnectionsPerUser);
            if (monitoringService != null) {
                monitoringService.incrementCounter("limiter.user_limit_reached", "userId:" + userId);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 记录新连接
     * @param userId 用户ID
     */
    public void recordConnection(String userId) {
        // 增加用户连接计数
        AtomicInteger count = userConnectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        
        // 增加全局连接计数
        int globalCount = globalConnectionCount.incrementAndGet();
        
        log.info("新连接已记录: userId={}, userCount={}, globalCount={}", userId, newCount, globalCount);
        
        // 记录监控指标
        if (monitoringService != null) {
            monitoringService.recordMetric("connections.user_count", newCount, "userId:" + userId);
            monitoringService.recordMetric("connections.global_count", globalCount);
        }
    }
    
    /**
     * 记录连接关闭
     * @param userId 用户ID
     */
    public void recordDisconnection(String userId) {
        // 减少用户连接计数
        AtomicInteger count = userConnectionCounts.get(userId);
        if (count != null) {
            int newCount = count.decrementAndGet();
            if (newCount <= 0) {
                userConnectionCounts.remove(userId);
            }
            
            // 减少全局连接计数
            int globalCount = globalConnectionCount.decrementAndGet();
            
            log.info("连接已关闭: userId={}, userCount={}, globalCount={}", userId, Math.max(0, newCount), globalCount);
            
            // 记录监控指标
            if (monitoringService != null) {
                monitoringService.recordMetric("connections.user_count", Math.max(0, newCount), "userId:" + userId);
                monitoringService.recordMetric("connections.global_count", globalCount);
            }
        }
    }
    
    /**
     * 设置单用户最大连接数
     */
    public void setMaxConnectionsPerUser(int maxConnectionsPerUser) {
        this.maxConnectionsPerUser = maxConnectionsPerUser;
        log.info("已更新单用户最大连接数: {}", maxConnectionsPerUser);
    }
    
    /**
     * 设置全局最大连接数
     */
    public void setMaxGlobalConnections(int maxGlobalConnections) {
        this.maxGlobalConnections = maxGlobalConnections;
        log.info("已更新全局最大连接数: {}", maxGlobalConnections);
    }
    
    /**
     * 获取当前全局连接数
     */
    public int getGlobalConnectionCount() {
        return globalConnectionCount.get();
    }
    
    /**
     * 获取指定用户的连接数
     */
    public int getUserConnectionCount(String userId) {
        AtomicInteger count = userConnectionCounts.get(userId);
        return count != null ? count.get() : 0;
    }
}