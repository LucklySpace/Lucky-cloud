package com.xy.lucky.crypto.core.sign.annotation.aspect;


import com.xy.lucky.crypto.core.sign.annotation.Signature;
import com.xy.lucky.crypto.core.sign.core.SignatureAlgorithm;
import com.xy.lucky.crypto.core.sign.domain.SignatureMode;
import com.xy.lucky.crypto.core.sign.utils.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Aspect
@Component
public class SignatureAspect {

    private Map<SignatureMode, SignatureAlgorithm> algorithmMap = new EnumMap<>(SignatureMode.class);

    @Autowired
    public SignatureAspect(List<SignatureAlgorithm> algorithms) {
        for (SignatureAlgorithm alg : algorithms) {
            algorithmMap.put(alg.mode(), alg);
        }
    }


    @Around("@annotation(signature)")
    public Object around(ProceedingJoinPoint pjp, Signature signature) throws Throwable {
        HttpServletRequest request = getRequest();

        // 验签逻辑
        if (signature.verify()) {
            Map<String, String> params = SignUtil.getParams(request);
            String clientSign = params.remove("sign");

            SignatureAlgorithm algorithm = algorithmMap.get(signature.mode());
            if (!algorithm.verify(params, clientSign)) {
                throw new SecurityException("签名验证失败");
            }
        }

        // 方法执行
        Object result = pjp.proceed();

        // 响应加签：当返回值为 Map 时，在原返回中追加签名字段
        if (signature.sign()) {
            SignatureAlgorithm algorithm = algorithmMap.get(signature.mode());
            if (result instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> mutable = (Map<Object, Object>) result;
                Map<String, String> toSign = new TreeMap<>();
                for (Map.Entry<Object, Object> e : mutable.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        toSign.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
                    }
                }
                String sign = algorithm.sign(toSign);
                mutable.put("sign", sign);
                return mutable;
            }
        }

        return result;
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
}
