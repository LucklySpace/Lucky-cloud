package com.xy.lucky.general.config;

import com.xy.lucky.general.response.handler.ResponseHandler;
import com.xy.lucky.general.version.core.ApiVersionRequestMappingHandlerMapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;

@Configuration
@EnableConfigurationProperties(ResponseHandlerProperties.class)
public class WebFluxConfig implements WebFluxConfigurer {

    @Value("${response.handler.exclude-paths}")
    private String[] excludePaths;

    @Bean("apiVersionRequestMappingHandlerMapping")
    public RequestMappingHandlerMapping apiVersionRequestMappingHandlerMapping() {
        ApiVersionRequestMappingHandlerMapping mapping = new ApiVersionRequestMappingHandlerMapping();
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    @Bean
    @Primary
    public ResponseBodyResultHandler resultHandler(@Qualifier("webFluxAdapterRegistry") ReactiveAdapterRegistry reactiveAdapterRegistry,
                                                   ServerCodecConfigurer serverCodecConfigurer,
                                                   @Qualifier("webFluxContentTypeResolver") RequestedContentTypeResolver contentTypeResolver,
                                                   ResponseHandlerProperties properties) {
        return new ResponseHandler(
                serverCodecConfigurer.getWriters(),
                contentTypeResolver,
                reactiveAdapterRegistry,
                properties.getExcludePaths()
        );
    }

}

