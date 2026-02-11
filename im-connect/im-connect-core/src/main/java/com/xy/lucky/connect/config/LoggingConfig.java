package com.xy.lucky.connect.config;

import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 统一日志配置
 * - 配置MDC上下文
 * - 统一日志格式和标准
 */
@Slf4j(topic = LogConstant.System)
@Component
public class LoggingConfig {

    // MDC常量
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_INSTANCE_ID = "instanceId";

    // 实例ID，用于区分多实例部署
    private final String instanceId = generateInstanceId();

    @PostConstruct
    public void init() {
        // 设置实例ID
        MDC.put(MDC_INSTANCE_ID, instanceId);
        
        log.info("日志配置初始化完成，实例ID: {}", instanceId);
        
        // 输出日志使用指南
        log.info("日志使用指南：");
        log.info("1. 请使用 LogConstant 中定义的日志主题");
        log.info("2. 请在处理用户请求前设置 MDC.put(LoggingConfig.MDC_USER_ID, userId)");
        log.info("3. 请在处理消息前设置 MDC.put(LoggingConfig.MDC_REQUEST_ID, requestId)");
        log.info("4. 请在处理完成后清理 MDC.remove(LoggingConfig.MDC_USER_ID) 等");
    }

    /**
     * 生成实例ID
     */
    private String generateInstanceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 设置当前线程的用户ID
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
    }

    /**
     * 设置当前线程的请求ID
     */
    public static void setRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(MDC_REQUEST_ID, requestId);
        }
    }

    /**
     * 设置当前线程的跟踪ID
     */
    public static void setTraceId(String traceId) {
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_TRACE_ID, traceId);
    }

    /**
     * 清理当前线程的MDC上下文
     */
    public static void clearContext() {
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_TRACE_ID);
    }
}