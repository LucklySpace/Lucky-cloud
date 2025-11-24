package com.xy.lucky.general.log;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 慢请求监控切面
 * 用于监控Spring Web接口的执行时间，如果超过阈值则记录警告日志。
 * 同时在异常时记录详细错误日志。
 * 日志信息包括：请求URL、方法全限定名、参数、执行时间、异常信息等，以确保完整性。
 */
@Aspect
@Component
public class SlowRequestAspect {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestAspect.class);

    /**
     * 慢请求阈值（毫秒），默认5秒，可通过配置文件调整。
     */
    @Value("${app.slow-request-threshold:5000}")
    private long threshold;

    @Resource
    private HttpServletRequest request;

    /**
     * 监控接口执行时间
     * 环绕Spring Web的常见Mapping注解，记录执行时间和相关信息。
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 如果方法执行抛出异常
     */
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object monitorSlowRequest(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取方法签名和参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getMethod().getName();
        Object[] args = joinPoint.getArgs();

        // 构建日志前缀：请求URL + 方法全限定名
        String requestUrl = (request != null) ? request.getMethod() + " " + request.getRequestURI() : "Unknown URL";
        String fullMethodName = className + "." + methodName;

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 计算执行时间
            long elapsedTime = System.currentTimeMillis() - startTime;

            // 如果超过阈值，记录警告日志
            if (elapsedTime > threshold) {
                log.warn("慢接口警告: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                        requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
            } else {
                // 可选：记录正常执行的info日志（如果需要完整追踪所有请求，可启用）
                log.info("接口执行: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                        requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
            }

            return result;
        } catch (Exception e) {
            // 计算执行时间
            long elapsedTime = System.currentTimeMillis() - startTime;

            // 记录错误日志，包括异常栈追踪
            log.error("接口执行异常: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms, 异常信息: {}",
                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime, e.getMessage(), e);

            throw e;
        }
    }
}