package com.xy.lucky.general.log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * 慢接口日志切面
 * <p>
 * 监控接口执行时间，超过指定阈值则记录警告日志
 */
@Aspect
@Component
public class SlowRequestAspect {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestAspect.class);

    @Value("${app.slow-request-threshold:5000}")
    private long threshold;

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object monitorSlowRequest(ProceedingJoinPoint joinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringTypeName();
        String methodName = signature.getMethod().getName();
        Object[] args = joinPoint.getArgs();

        String requestUrl = resolveRequestUrl(args);
        String fullMethodName = className + "." + methodName;

        long startTime = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return mono.doOnError(e -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        log.error("接口执行异常: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms, 异常信息: {}",
                                requestUrl, fullMethodName, Arrays.toString(args), elapsedTime, e.getMessage(), e);
                    })
                    .doFinally(signal -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        if (elapsedTime > threshold) {
                            log.warn("慢接口警告: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
                        } else {
                            log.info("接口执行: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
                        }
                    });
        }

        if (result instanceof Flux<?> flux) {
            return flux.doOnError(e -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        log.error("接口执行异常: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms, 异常信息: {}",
                                requestUrl, fullMethodName, Arrays.toString(args), elapsedTime, e.getMessage(), e);
                    })
                    .doFinally(signal -> {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        if (elapsedTime > threshold) {
                            log.warn("慢接口警告: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
                        } else {
                            log.info("接口执行: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
                        }
                    });
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > threshold) {
            log.warn("慢接口警告: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
        } else {
            log.info("接口执行: URL=[{}], Method=[{}], Params=[{}], 执行时间: {}ms",
                    requestUrl, fullMethodName, Arrays.toString(args), elapsedTime);
        }
        return result;
    }

    private String resolveRequestUrl(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ServerWebExchange exchange) {
                return exchange.getRequest().getMethod() + " " + exchange.getRequest().getURI();
            }
            if (arg instanceof ServerHttpRequest req) {
                return req.getMethod() + " " + req.getURI();
            }
        }
        return "Unknown URL";
    }
}
