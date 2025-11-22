package com.xy.lucky.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 路由缓存配置（Caffeine）与路由仓库 Bean 注册
 *
 * 支持通过 application.yml 注入 dataId / group / timeout 参数，示例：
 *
 * spring:
 *   gateway:
 *     nacos:
 *       data-id: gateway-routes.yml
 *       group: DEFAULT_GROUP
 *       timeout-ms: 5000
 */
@Configuration
public class RouteCacheConfig {

    @Bean
    public Cache<Object, Object> routeCache() {
        // 本地缓存仅保存已解析好的 RouteDefinition 列表，读多写少
        return Caffeine.newBuilder()
                .maximumSize(1) // 只缓存一个键（CACHE_KEY），列表里包含所有路由
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public RouteDefinitionRepository routeDefinitionRepository(
            Cache<Object, Object> routeCache,
            NacosConfigManager nacosConfigManager,
            @Value("${spring.gateway.nacos.data-id:gateway-routes.yml}") String dataId,
            @Value("${spring.gateway.nacos.group:DEFAULT_GROUP}") String group,
            @Value("${spring.gateway.nacos.timeout-ms:5000}") long timeoutMs) {

        return new CachedRouteDefinitionRepository(routeCache, nacosConfigManager, dataId, group, timeoutMs);
    }
}