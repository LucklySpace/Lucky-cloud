package com.xy.auth.annotations.count;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;


/**
 * 记录接口耗时
 */
@Slf4j
@Aspect
@Component
public class TakeCountAspect {

    //用threadlocal记录当前线程的开始访问时间
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Before("@annotation(takeCount)")
    public void doBefore(TakeCount takeCount) {
        //记录开始时间
        startTime.set(System.currentTimeMillis());
    }

    @After("@annotation(TakeCount)")
    public void doAfter(JoinPoint point) {
        log.info("{}访问耗时为：{}ms", point.getSignature().getName(), (System.currentTimeMillis() - startTime.get()));
    }
}