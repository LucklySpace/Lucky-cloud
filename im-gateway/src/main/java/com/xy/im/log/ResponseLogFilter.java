package com.xy.im.log;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Configuration
@ConditionalOnProperty(value = "log.request.enabled", havingValue = "true", matchIfMissing = true)
public class ResponseLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 打印请求路径
        String path = request.getPath().pathWithinApplication().value();
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {

                    MultiValueMap<String, String> queryParams = request.getQueryParams();
                    String requestUrl = UriComponentsBuilder.fromPath(path).queryParams(queryParams).build().toUriString();

                    // 构建成一条长日志
                    StringBuilder responseLog = new StringBuilder(200);

                    // 日志参数
                    List<Object> responseArgs = new ArrayList<>();
                    responseLog.append("\n\n================ Gateway Response Start  ================\n");
                    ServerHttpResponse response = exchange.getResponse();

                    // 状态码个path占位符: 200 get: /xxx/xxx/xxx?a=b
                    responseLog.append("<=== {} {}: {}\n");

                    // 参数
                    String requestMethod = request.getMethod().toString();
                    responseArgs.add(response.getStatusCode().value());
                    responseArgs.add(requestMethod);
                    responseArgs.add(requestUrl);

                    // 打印请求头
                    //HttpHeaders headers = response.getHeaders();
                    //headers.forEach((headerName, headerValue) -> {
                    //    responseLog.append("===Headers===  {}: {}\n");
                    //    responseArgs.add(headerName);
                    //    responseArgs.add(StringUtils.join(headerValue.toArray()));
                    //});

                    responseLog.append("================  Gateway Response End  =================\n");
                    // 打印执行时间
                    log.info(responseLog.toString(), responseArgs.toArray());
                })
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
