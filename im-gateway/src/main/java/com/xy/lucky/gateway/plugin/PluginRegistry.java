package com.xy.lucky.gateway.plugin;

import com.xy.lucky.gateway.config.GatewayPluginProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PluginRegistry {

    private final List<GatewayPlugin> pluginBeans;
    private final GatewayPluginProperties properties;
    private final AtomicReference<List<PluginRuntime>> runtimesRef = new AtomicReference<>(List.of());

    @PostConstruct
    public void init() {
        reload();
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        if (!properties.isHotReload()) {
            return;
        }
        reload();
    }

    public List<PluginRuntime> getRuntimes() {
        return runtimesRef.get();
    }

    public synchronized void reload() {
        if (!properties.isEnabled()) {
            runtimesRef.set(List.of());
            log.warn("网关插件框架已禁用");
            return;
        }
        Map<String, GatewayPluginProperties.Definition> definitions = properties.getDefinitions().stream()
                .filter(d -> StringUtils.hasText(d.getId()))
                .collect(Collectors.toMap(d -> d.getId().trim(), Function.identity(), (a, b) -> b));

        List<PluginRuntime> runtimes = new ArrayList<>();
        for (GatewayPlugin plugin : pluginBeans) {
            GatewayPluginProperties.Definition definition = definitions.get(plugin.getId());
            boolean enabled = definition != null ? definition.isEnabled() : plugin.isEnabledByDefault();
            if (!enabled) {
                continue;
            }

            if (properties.isStrictVersionCheck() && definition != null
                    && StringUtils.hasText(definition.getVersion())
                    && !definition.getVersion().trim().equals(plugin.getVersion())) {
                log.warn("插件版本不匹配，已跳过: pluginId={}, configured={}, actual={}",
                        plugin.getId(), definition.getVersion(), plugin.getVersion());
                continue;
            }

            int order = definition != null && definition.getOrder() != null ? definition.getOrder() : plugin.getOrder();
            long timeoutMs = definition != null && definition.getTimeoutMs() != null && definition.getTimeoutMs() > 0
                    ? definition.getTimeoutMs() : properties.getDefaultTimeoutMs();
            int maxConcurrency = definition != null && definition.getMaxConcurrency() != null && definition.getMaxConcurrency() > 0
                    ? definition.getMaxConcurrency() : properties.getDefaultMaxConcurrency();
            boolean failOpen = definition != null && definition.getFailOpen() != null
                    ? definition.getFailOpen() : properties.isDefaultFailOpen();
            List<String> includePaths = definition != null ? definition.getIncludePaths() : List.of();
            List<String> excludePaths = definition != null ? definition.getExcludePaths() : List.of();

            runtimes.add(new PluginRuntime(plugin, order, timeoutMs, failOpen, maxConcurrency, includePaths, excludePaths));
        }

        List<PluginRuntime> ordered = runtimes.stream()
                .sorted(Comparator.comparingInt(PluginRuntime::getOrder).thenComparing(PluginRuntime::getId))
                .toList();
        runtimesRef.set(ordered);
        log.info("网关插件链已刷新，启用插件数量={}", ordered.size());
    }
}
