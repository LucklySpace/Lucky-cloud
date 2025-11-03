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
 * 消息速率限制器
 * - 限制单个用户的消息发送速率
 * - 提供滑动窗口计数
 * - 集成监控告警
 */
@Slf4j(topic = LogConstant.Limiter)
@Component
public class MessageRateLimiter {

    // 默认窗口大小（毫秒）
    private static final long DEFAULT_WINDOW_SIZE_MS = 1000;
    
    // 默认单用户窗口内最大消息数
    private static final int DEFAULT_MAX_MESSAGES_PER_WINDOW = 20;
    
    // 窗口大小（毫秒）
    private long windowSizeMs = DEFAULT_WINDOW_SIZE_MS;
    
    // 单用户窗口内最大消息数
    private int maxMessagesPerWindow = DEFAULT_MAX_MESSAGES_PER_WINDOW;
    
    // 用户消息计数窗口
    private final Map<String, UserWindow> userWindows = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private MonitoringService monitoringService;
    
    /**
     * 检查用户是否可以发送新消息
     * @param userId 用户ID
     * @return 是否允许发送
     */
    public boolean allowMessage(String userId) {
        long now = System.currentTimeMillis();
        
        // 获取或创建用户窗口
        UserWindow window = userWindows.computeIfAbsent(userId, k -> new UserWindow(now));
        
        // 如果窗口已过期，重置窗口
        if (now - window.getStartTime() > windowSizeMs) {
            window.reset(now);
        }
        
        // 检查是否超过限制
        if (window.getCount() >= maxMessagesPerWindow) {
            log.warn("用户消息速率超限: userId={}, count={}, limit={}", 
                    userId, window.getCount(), maxMessagesPerWindow);
            
            if (monitoringService != null) {
                monitoringService.incrementCounter("rate_limiter.limit_exceeded", "userId:" + userId);
            }
            
            return false;
        }
        
        // 增加计数
        window.incrementCount();
        return true;
    }
    
    /**
     * 设置窗口大小（毫秒）
     */
    public void setWindowSizeMs(long windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
        log.info("已更新消息窗口大小: {}ms", windowSizeMs);
    }
    
    /**
     * 设置单用户窗口内最大消息数
     */
    public void setMaxMessagesPerWindow(int maxMessagesPerWindow) {
        this.maxMessagesPerWindow = maxMessagesPerWindow;
        log.info("已更新单用户窗口内最大消息数: {}", maxMessagesPerWindow);
    }
    
    /**
     * 用户窗口
     */
    private static class UserWindow {
        private long startTime;
        private final AtomicInteger count = new AtomicInteger(0);
        
        public UserWindow(long startTime) {
            this.startTime = startTime;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public int getCount() {
            return count.get();
        }
        
        public int incrementCount() {
            return count.incrementAndGet();
        }
        
        public void reset(long newStartTime) {
            this.startTime = newStartTime;
            this.count.set(0);
        }
    }
}