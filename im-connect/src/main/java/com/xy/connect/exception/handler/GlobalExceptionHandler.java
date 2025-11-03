package com.xy.connect.exception.handler;

import com.xy.connect.config.LogConstant;
import com.xy.connect.exception.MqException;
import com.xy.connect.exception.NacosException;
import com.xy.connect.exception.NettyException;
import com.xy.connect.monitoring.MonitoringService;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.exception.CyclicDependencyException;
import com.xy.spring.exception.NoSuchBeanException;
import com.xy.spring.exception.handler.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器
 * - 分类处理不同类型的异常
 * - 集成监控报警
 * - 提供异常恢复策略
 */
@Slf4j(topic = LogConstant.Exception)
@Component
public class GlobalExceptionHandler implements ExceptionHandler {

    @Autowired(required = false)
    private MonitoringService monitoringService;

    @Override
    public void handle(Exception ex, String context) {
        // 1. 框架类异常
        if (ex instanceof NoSuchBeanException) {
            // 记录并快速退出，不做报警
            log.warn("[框架异常] {}: {}", context, ex.getMessage());
        } else if (ex instanceof CyclicDependencyException) {
            // 重要异常：上报并可能触发熔断
            log.error("[严重框架异常] 循环依赖 in {}: {}", context, ex.getMessage());
            reportCritical(context, ex);
        }
        // 2. 网络类异常
        else if (ex instanceof SocketException) {
            log.warn("[网络异常] 连接问题 in {}: {}", context, ex.getMessage());
            reportWarning(context, "网络连接异常", ex);
        } else if (ex instanceof TimeoutException) {
            log.warn("[网络异常] 超时 in {}: {}", context, ex.getMessage());
            reportWarning(context, "网络超时", ex);
        } else if (ex instanceof IOException) {
            log.error("[IO异常] in {}: {}", context, ex.getMessage());
            reportError(context, ex);
        }
        // 3. 业务类异常
        else if (ex instanceof NettyException) {
            log.error("[Netty异常] in {}: {}", context, ex.getMessage());
            reportError(context, ex);
        } else if (ex instanceof MqException) {
            log.error("[消息队列异常] in {}: {}", context, ex.getMessage());
            reportError(context, ex);
        } else if (ex instanceof NacosException) {
            log.error("[服务发现异常] in {}: {}", context, ex.getMessage());
            reportCritical(context, ex);
        }
        // 4. 未分类异常
        else {
            log.error("[未处理异常] in {}: ", context, ex);
            reportError(context, ex);
        }
    }

    /**
     * 报告严重异常 - 可能触发熔断或告警
     */
    private void reportCritical(String context, Exception ex) {
        if (monitoringService != null) {
            monitoringService.reportCritical(context, ex);
        }
    }

    /**
     * 报告一般错误
     */
    private void reportError(String context, Exception ex) {
        if (monitoringService != null) {
            monitoringService.reportError(context, ex);
        }
    }

    /**
     * 报告警告级别问题
     */
    private void reportWarning(String context, String message, Exception ex) {
        if (monitoringService != null) {
            monitoringService.reportWarning(context, message, ex);
        }
    }
}
