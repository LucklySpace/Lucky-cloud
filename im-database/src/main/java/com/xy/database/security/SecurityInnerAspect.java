package com.xy.database.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpHeaders;

import static com.xy.core.constants.IMConstant.INTERNAL_CALL_FLAG;

@Slf4j
@Aspect
@AllArgsConstructor
public class SecurityInnerAspect {

    private final HttpServletRequest request;

    @SneakyThrows
    @Around("@annotation(inner)")
    public Object around(ProceedingJoinPoint point, SecurityInner inner) {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (inner.value() && !StrUtil.equals(INTERNAL_CALL_FLAG, header)) {
            log.warn("访问接口 {} 没有权限", point.getSignature().getName());
            throw new Exception("Access is denied");
        } else {
            log.info("openfeign访问接口 {} ", point.getSignature().getName());
        }

        return point.proceed();
    }
}