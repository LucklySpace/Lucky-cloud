//package com.xy.server.annotations.lock;
//
//import jakarta.annotation.Resource;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.reflect.MethodSignature;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.lang.reflect.Method;
//import java.util.concurrent.TimeUnit;
//
//@Aspect
//@Component
//public class LockTransactionalAspect {
//
//    @Resource
//    private RedissonClient redissonClient;
//
//    @Around("@annotation(LockTransactional)")
//    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
//        // 获取方法签名以及注解
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        Method method = signature.getMethod();
//        LockTransactional lockTransactional = method.getAnnotation(LockTransactional.class);
//        if (lockTransactional == null) {
//            // 如果方法上没有注解，则尝试获取类上的注解
//            lockTransactional = joinPoint.getTarget().getClass().getAnnotation(LockTransactional.class);
//        }
//        // 提取分布式锁 key，若未设置，则默认使用方法签名作为 key
//        String lockKey = lockTransactional.key();
//        if (!StringUtils.hasText(lockKey)) {
//            lockKey = joinPoint.getSignature().toShortString();
//        }
//
//        // 从 RedissonClient 获取 RLock 对象
//        RLock lock = redissonClient.getLock(lockKey);
//        boolean acquired = false;
//        try {
//            // 尝试加锁，这里等待 10 秒，加锁后 30 秒自动释放（可根据需要调整）
//            acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
//            if (!acquired) {
//                throw new RuntimeException("无法获取分布式锁，锁 key：" + lockKey);
//            }
//            // 获取锁成功后，执行业务逻辑
//            return joinPoint.proceed();
//        } finally {
//            // 释放锁
//            if (acquired && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
//}
