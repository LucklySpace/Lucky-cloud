package com.xy.lucky.gateway.plugin;

import com.xy.lucky.gateway.utils.ResponseUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;

@Slf4j
@Getter
public class PluginRuntime {

    private final GatewayPlugin plugin;
    private final int order;
    private final long timeoutMs;
    private final boolean failOpen;
    private final List<String> includePaths;
    private final List<String> excludePaths;
    private final Semaphore semaphore;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PluginRuntime(GatewayPlugin plugin,
                         int order,
                         long timeoutMs,
                         boolean failOpen,
                         int maxConcurrency,
                         List<String> includePaths,
                         List<String> excludePaths) {
        this.plugin = plugin;
        this.order = order;
        this.timeoutMs = timeoutMs;
        this.failOpen = failOpen;
        this.semaphore = new Semaphore(Math.max(1, maxConcurrency));
        this.includePaths = includePaths;
        this.excludePaths = excludePaths;
    }

    public String getId() {
        return plugin.getId();
    }

    public boolean matchesPath(String path) {
        if (!CollectionUtils.isEmpty(excludePaths) && excludePaths.stream().anyMatch(p -> pathMatcher.match(p, path))) {
            return false;
        }
        if (CollectionUtils.isEmpty(includePaths)) {
            return true;
        }
        return includePaths.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    public Mono<Void> invoke(ServerWebExchange exchange, GatewayPluginChain next) {
        if (!semaphore.tryAcquire()) {
            if (failOpen) {
                log.warn("插件并发上限触发，降级放行: pluginId={}", getId());
                return next.filter(exchange);
            }
            return ResponseUtil.writeJson(exchange, HttpStatus.SERVICE_UNAVAILABLE, "PLUGIN_BUSY_" + getId());
        }

        Mono<Void> invokeMono = plugin.apply(exchange, next);
        if (timeoutMs > 0) {
            invokeMono = invokeMono.timeout(Duration.ofMillis(timeoutMs));
        }

        return invokeMono
                .onErrorResume(ex -> {
                    if (failOpen) {
                        log.error("插件执行异常，降级放行: pluginId={}", getId(), ex);
                        if (exchange.getResponse().isCommitted()) {
                            return Mono.empty();
                        }
                        return next.filter(exchange);
                    }
                    log.error("插件执行异常，拒绝请求: pluginId={}", getId(), ex);
                    return ResponseUtil.writeJson(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "PLUGIN_ERROR_" + getId());
                })
                .doFinally(signalType -> semaphore.release());
    }
}
