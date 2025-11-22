package com.xy.lucky.server.api.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Target;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import java.util.Objects;

import static com.xy.lucky.core.constants.IMConstant.INTERNAL_CALL_FLAG;

/**
 * 内部调用请求头
 */
@Slf4j
@Configuration
public class FeignRequestInterceptor implements RequestInterceptor {


    @Override
    public void apply(RequestTemplate template) {

        // 添加请求头
        template.header(HttpHeaders.AUTHORIZATION, INTERNAL_CALL_FLAG);

        // 日志输出
        if (Objects.nonNull(template.feignTarget())) {

            Target<?> target = template.feignTarget();

            log.info("Openfeign service invoke,Request service name:[{}], Request url:[{}] Invoke class:[{}] ", target.name(), target.url(), target.type().getName());
        }

    }


    @Bean
    public Decoder feignDecoder() {
        MyJackson2HttpMessageConverter converter = new MyJackson2HttpMessageConverter();
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(converter);
        return new SpringDecoder(objectFactory);
    }


}