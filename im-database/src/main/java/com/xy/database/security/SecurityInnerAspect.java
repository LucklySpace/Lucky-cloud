package com.xy.database.security;

import cn.hutool.core.util.StrUtil;
import com.xy.database.exception.ForbiddenException;
import com.xy.general.response.domain.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpHeaders;

import static com.xy.core.constants.IMConstant.INTERNAL_CALL_FLAG;

/**
 * 内部调用安全切面
 * 用于验证请求是否来自内部服务调用
 */
@Slf4j
@Aspect
@AllArgsConstructor
public class SecurityInnerAspect {

    private final HttpServletRequest request;

    /**
     * 环绕通知，处理带有@SecurityInner注解的方法
     *
     * @param point 切点
     * @param inner 注解信息
     * @return 方法执行结果
     * @throws Throwable 异常信息
     */
    @Around("@annotation(inner)")
    public Object around(ProceedingJoinPoint point, SecurityInner inner) throws Throwable {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 如果需要验证内部调用权限且请求头不匹配，则抛出异常
        if (inner.value() && !StrUtil.equals(INTERNAL_CALL_FLAG, header)) {
            log.warn("访问接口 {} 没有权限", point.getSignature().getName());
            throw new ForbiddenException(ResultCode.FORBIDDEN);
        } else {
            log.info("openfeign访问接口 {} ", point.getSignature().getName());
        }

        return point.proceed();
    }
}