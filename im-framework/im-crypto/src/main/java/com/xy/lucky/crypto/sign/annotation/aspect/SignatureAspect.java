package com.xy.lucky.crypto.sign.annotation.aspect;


import com.xy.lucky.crypto.sign.annotation.Signature;
import com.xy.lucky.crypto.sign.core.SignatureAlgorithm;
import com.xy.lucky.crypto.sign.domain.SignatureMode;
import com.xy.lucky.crypto.sign.utils.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        System.out.println("get请求的advice触发了");

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

        // 响应签名（如果返回为 Map 或支持包裹签名）
        if (signature.sign()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = new HashMap<>();
            map.put("userId", "100001");
            map.put("token", "g7632d867fyuh49w57");
            SignatureAlgorithm algorithm = algorithmMap.get(signature.mode());
            String sign = algorithm.sign(map);
            map.put("sign", sign);
        }

        return result;
    }

    private HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }
}
