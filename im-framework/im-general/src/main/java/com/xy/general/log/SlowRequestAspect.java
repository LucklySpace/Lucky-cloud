package com.xy.general.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SlowRequestAspect {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestAspect.class);

    /**
     * 慢请求阈值，默认5秒
     */
    @Value("${app.slow-request-threshold:5000}")
    private long threshold;

    /**
     * 监控接口执行时间
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object monitorSlowRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (elapsedTime > threshold) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                log.warn("慢接口警告: {} 执行时间: {}ms",
                        signature.getMethod().getName(), elapsedTime);
            }

            return result;
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            log.error("接口执行异常: {} 执行时间: {}ms, 异常信息: {}",
                    signature.getMethod().getName(), elapsedTime, e.getMessage());
            throw e;
        }
    }
}