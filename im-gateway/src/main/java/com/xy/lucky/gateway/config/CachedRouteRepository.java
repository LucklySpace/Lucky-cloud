package com.xy.lucky.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 路由缓存实现
 * <p>
 * - 优先从本地缓存读取（cacheKey = "routes"）
 * - 缓存未命中时，从 Nacos 拉取（阻塞调用在 boundedElastic 线程池执行）
 * - 构造时注册 Nacos 监听器，变更时刷新缓存（最终一致性）
 * - 支持常见的两种 routes 写法（字符串或 Map）
 */
@Slf4j
@Configuration
public class CachedRouteRepository implements RouteDefinitionRepository {

    private final Yaml yaml = new Yaml();

    /**
     * 监听器是否已注册
     */
    private final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    /**
     * Nacos 监听器线程池
     */
    private final Executor listenerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nacos-route-listener");
        t.setDaemon(true);
        return t;
    });

    /**
     * 路由缓存
     */
    private final Cache<String, List<RouteDefinition>> cache = Caffeine.newBuilder()
            .maximumSize(1) // 只缓存一个键（CACHE_KEY），列表里包含所有路由
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    private final NacosConfigManager nacosConfigManager;
    @Value("${lucky.gateway.route-cache.data-id}")
    private String dataId;

    @Value("${lucky.gateway.route-cache.cache-key:routes}")
    private String CACHE_KEY = "routes";

    public CachedRouteRepository(NacosConfigManager nacosConfigManager) {
        this.nacosConfigManager = nacosConfigManager;
    }

    @Value("${lucky.gateway.route-cache.group:DEFAULT_GROUP}")
    private String group;

    @Value("${lucky.gateway.route-cache.timeout-ms:5000}")
    private long timeoutMillis;

    // 安全转换辅助
    @SuppressWarnings("unchecked")
    private static <T> T safeCast(Object obj, Class<T> clazz) {
        if (obj == null) return null;
        if (clazz.isInstance(obj)) return (T) obj;
        return null;
    }

    // 获取嵌套 Map（支持多个层级）
    @SuppressWarnings("unchecked")
    private static Map<String, Object> getNestedMap(Map<String, Object> root, String... keys) {
        Map<String, Object> cur = root;
        for (String key : keys) {
            Object o = cur.get(key);
            if (!(o instanceof Map)) return null;
            cur = (Map<String, Object>) o;
        }
        return cur;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<RouteDefinition> cached = cache.getIfPresent(CACHE_KEY);
        if (!CollectionUtils.isEmpty(cached)) {
            log.debug("从缓存命中路由，共 {} 条", cached.size());
            return Flux.fromIterable(cached);
        }

        // 缓存未命中：在 boundedElastic 池中阻塞调用 Nacos
        return Mono.fromCallable(this::loadFromNacosAndCache)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .timeout(Duration.ofSeconds(Math.max(5, timeoutMillis / 1000)),
                        Flux.empty()); // 防御性超时：若超时返回空流
    }

    // 从 Nacos 读取配置并解析、缓存
    private List<RouteDefinition> loadFromNacosAndCache() {
        try {
            log.debug("从 Nacos 读取路由配置，dataId={}, group={}", dataId, group);
            ConfigService cs = nacosConfigManager.getConfigService();
            String config = cs.getConfig(dataId, group, timeoutMillis);
            if (!StringUtils.hasText(config)) {
                List<RouteDefinition> local = loadFromClasspath(dataId);
                if (!local.isEmpty()) {
                    cache.put(CACHE_KEY, local);
                    log.info("[Nacos Route] 使用本地 classpath 路由，数量={}", local.size());
                    return local;
                }
                log.warn("[Nacos Route] 配置为空：dataId={}, group={}", dataId, group);
                return Collections.emptyList();
            }
            List<RouteDefinition> list = parseYamlToRoutes(config);
            cache.put(CACHE_KEY, list);
            log.info("加载并缓存路由，共 {} 条", list.size());
            return list;
        } catch (Exception e) {
            log.error("从 Nacos 加载路由失败", e);
            return Collections.emptyList();
        }
    }

    // 解析 YAML -> RouteDefinition 列表（支持 spring.cloud.gateway.routes 两种写法）
    @SuppressWarnings("unchecked")
    private List<RouteDefinition> parseYamlToRoutes(String ymlContent) {
        if (!StringUtils.hasText(ymlContent)) return Collections.emptyList();
        try {
            Object loaded = yaml.load(ymlContent);
            Map<String, Object> root = safeCast(loaded, Map.class);
            if (root == null) {
                log.warn("路由配置顶层不是 Map，忽略");
                return Collections.emptyList();
            }

            Map<String, Object> routesParent = getNestedMap(root, "spring", "cloud", "gateway");
            if (routesParent == null) {
                log.warn("路由配置未包含 spring.cloud.gateway 节点");
                return Collections.emptyList();
            }

            List<?> routesObj = safeCast(routesParent.get("routes"), List.class);
            if (CollectionUtils.isEmpty(routesObj)) {
                log.warn("spring.cloud.gateway.routes 缺失或为空");
                return Collections.emptyList();
            }

            List<RouteDefinition> result = new ArrayList<>();
            for (Object r : routesObj) {
                Map<String, Object> map = safeCast(r, Map.class);
                if (map == null) {
                    log.warn("单个路由条目不是 Map，跳过：{}", r);
                    continue;
                }
                try {
                    RouteDefinition rd = mapToRouteDefinition(map);
                    if (rd != null && StringUtils.hasText(rd.getId())) {
                        result.add(rd);
                    } else {
                        log.warn("解析到无效路由（缺少 id），跳过：{}", map);
                    }
                } catch (Exception ex) {
                    log.error("解析单个路由定义失败，跳过该路由：{}", map, ex);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("解析路由 YML 失败", e);
            return Collections.emptyList();
        }
    }

    // 将 Map -> RouteDefinition（简洁实现）
    @SuppressWarnings("unchecked")
    private RouteDefinition mapToRouteDefinition(Map<String, Object> map) {
        Object idObj = map.get("id");
        if (idObj == null) return null;

        RouteDefinition rd = new RouteDefinition();
        rd.setId(String.valueOf(idObj));

        // uri (可选)
        Object uri = map.get("uri");
        if (uri != null && StringUtils.hasText(String.valueOf(uri))) {
            try {
                rd.setUri(URI.create(String.valueOf(uri)));
            } catch (Exception e) {
                log.warn("非法的 URI 格式：{} for route {}", uri, rd.getId());
            }
        }

        // predicates & filters
        rd.setPredicates(parsePredicateList(safeCast(map.get("predicates"), List.class)));
        rd.setFilters(parseFilterList(safeCast(map.get("filters"), List.class)));

        // metadata
        Map<String, Object> metadata = safeCast(map.get("metadata"), Map.class);
        if (!CollectionUtils.isEmpty(metadata)) {
            rd.setMetadata(metadata);
        }

        // order
        Object order = map.get("order");
        if (order != null) {
            try {
                rd.setOrder(Integer.parseInt(String.valueOf(order)));
            } catch (NumberFormatException ignored) {
            }
        }
        return rd;
    }

    // 通用：解析 predicates 列表
    private List<PredicateDefinition> parsePredicateList(List<?> list) {
        if (CollectionUtils.isEmpty(list)) return Collections.emptyList();
        return list.stream()
                .map(this::toPredicateDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 通用：解析 filters 列表
    private List<FilterDefinition> parseFilterList(List<?> list) {
        if (CollectionUtils.isEmpty(list)) return Collections.emptyList();
        return list.stream()
                .map(this::toFilterDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 将单个 predicate object -> PredicateDefinition
    @SuppressWarnings("unchecked")
    private PredicateDefinition toPredicateDefinition(Object obj) {
        if (obj == null) return null;
        PredicateDefinition pd = new PredicateDefinition();
        if (obj instanceof String s) {
            String[] parts = s.split("=", 2);
            pd.setName(parts[0].trim());
            if (parts.length > 1) pd.setArgs(singleArgMap(parts[1].trim()));
            return pd;
        }
        Map<String, Object> map = safeCast(obj, Map.class);
        if (map == null || map.isEmpty()) return null;
        Map.Entry<?, ?> e = map.entrySet().iterator().next();
        pd.setName(String.valueOf(e.getKey()));
        Object val = e.getValue();
        pd.setArgs(parseArgs(val));
        return pd;
    }

    // 将单个 filter object -> FilterDefinition
    @SuppressWarnings("unchecked")
    private FilterDefinition toFilterDefinition(Object obj) {
        if (obj == null) return null;
        FilterDefinition fd = new FilterDefinition();
        if (obj instanceof String s) {
            String[] parts = s.split("=", 2);
            fd.setName(parts[0].trim());
            if (parts.length > 1) fd.setArgs(singleArgMap(parts[1].trim()));
            return fd;
        }
        Map<String, Object> map = safeCast(obj, Map.class);
        if (map == null || map.isEmpty()) return null;
        Map.Entry<?, ?> e = map.entrySet().iterator().next();
        fd.setName(String.valueOf(e.getKey()));
        Object val = e.getValue();
        fd.setArgs(parseArgs(val));
        return fd;
    }

    private Map<String, String> singleArgMap(String v) {
        return Collections.singletonMap("_genkey_0", v);
    }

    // 注册 Nacos 配置变更监听器（只注册一次）
    @PostConstruct
    private void registerNacosListener() {
        if (!listenerRegistered.compareAndSet(false, true)) {
            return;
        }
        try {
            ConfigService configService = nacosConfigManager.getConfigService();

            if (configService == null) {
                log.warn("未能获取 Nacos ConfigService，跳过 listener 注册");
                return;
            }
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return listenerExecutor;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("[Nacos Route] 配置变更，刷新缓存 dataId={}, group={}", dataId, group);
                    try {
                        List<RouteDefinition> list = StringUtils.hasText(configInfo)
                                ? parseYamlToRoutes(configInfo)
                                : loadFromClasspath(dataId);
                        cache.put(CACHE_KEY, list);
                        log.info("[Nacos Route] 缓存已刷新，路由条数={}", list.size());
                    } catch (Exception e) {
                        log.error("[Nacos Route] 刷新缓存失败", e);
                    }
                }
            });
            log.debug("已为 Nacos dataId={} group={} 注册 listener", dataId, group);
        } catch (Throwable e) {
            log.error("注册 Nacos listener 失败", e);
        }
    }

    // 把单个值（String 或 List）解析成 args Map
    private Map<String, String> parseArgs(Object val) {
        switch (val) {
            case null -> {
                return Collections.emptyMap();
            }
            case String s -> {
                return singleArgMap(s);
            }
            case List<?> list -> {
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < list.size(); i++) {
                    map.put("_genkey_" + i, String.valueOf(list.get(i)));
                }
                return map;
            }
            default -> {
                return Collections.singletonMap("_genkey_0", String.valueOf(val));
            }
        }
    }

    // 从 classpath 加载默认路由文件（dataId 或 gateway-routes.yml）
    private List<RouteDefinition> loadFromClasspath(String resourceName) {
        List<RouteDefinition> empty = Collections.emptyList();
        try {
            ClassPathResource resource = new ClassPathResource(resourceName);
            if (!resource.exists()) {
                resource = new ClassPathResource(dataId);
                if (!resource.exists()) return empty;
            }
            try (InputStream in = resource.getInputStream()) {
                String yml = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
                if (!StringUtils.hasText(yml)) return empty;
                return parseYamlToRoutes(yml);
            }
        } catch (Exception e) {
            log.error("从 classpath 加载路由失败", e);
            return empty;
        }
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        log.warn("save 不支持（当前实现为只读缓存 + Nacos 驱动）");
        return Mono.empty();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        log.warn("delete 不支持（当前实现为只读缓存 + Nacos 驱动）");
        return Mono.empty();
    }
}

