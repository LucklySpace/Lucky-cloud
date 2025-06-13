package com.xy.auth.api;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.Objects;

import static com.xy.imcore.constants.IMConstant.IM_OPENFEIFN_INTER_CALL;

/**
 * 内部调用请求头
 */
@Slf4j
@Configuration
public class FeignRequestInterceptor implements RequestInterceptor {


    @Override
    public void apply(RequestTemplate template) {

        // 添加请求头
        template.header(HttpHeaders.AUTHORIZATION, IM_OPENFEIFN_INTER_CALL);

        // 日志输出
        if (Objects.nonNull(template.feignTarget())) {

            Target<?> target = template.feignTarget();

            log.info("Openfeign service invoke,Request service name:[{}], Request url:[{}] Invoke class:[{}] ", target.name(), target.url(), target.type().getName());
        }

    }

}