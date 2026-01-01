package com.xy.lucky.gateway.config;


import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 基于 Caffeine 的路由定义仓库实现（优化版）
 *
 * 特性：
 *  - 优先从本地缓存读取（cacheKey = "routes"）
 *  - 缓存未命中时，从 Nacos 拉取（阻塞调用在 boundedElastic 线程池执行）
 *  - 在构造时向 Nacos 注册 Listener，配置变更时自动刷新缓存（最终一致性）
 *  - 解析支持常见的两种 routes 写法（字符串或 Map）
 *
 * 注意：
 *  - 这个类不实现 save/delete（如需支持可扩展）
 *  - 构造时需要 dataId 与 group，默认超时时间可配置
 */
public class CachedRouteDefinitionRepository implements RouteDefinitionRepository {

    private static final Logger log = LoggerFactory.getLogger(CachedRouteDefinitionRepository.class);

    private static final String CACHE_KEY = "routes";

    private final Cache<Object, Object> cache;
    private final NacosConfigManager nacosConfigManager;
    private final Yaml yaml = new Yaml();
    private final String dataId;
    private final String group;
    private final long timeoutMillis;

    // 防止重复注册 listener
    private final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

    // 用于 Nacos Listener 的后台线程池（简单独立池，避免与 Reactor 线程混淆）
    private final Executor listenerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nacos-route-listener");
        t.setDaemon(true);
        return t;
    });

    /**
     * @param cache Caffeine cache（建议注入）
     * @param nacosConfigManager NacosConfigManager
     * @param dataId 要监听的 DataId，例如：gateway-routes.yml
     * @param group Nacos group，如 DEFAULT_GROUP
     * @param timeoutMillis getConfig 超时时间（毫秒）
     */
    public CachedRouteDefinitionRepository(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache,
                                           NacosConfigManager nacosConfigManager,
                                           String dataId,
                                           String group,
                                           long timeoutMillis) {
        this.cache = cache;
        this.nacosConfigManager = nacosConfigManager;
        this.dataId = dataId;
        this.group = group;
        this.timeoutMillis = timeoutMillis;
        registerNacosListener();
    }

    /**
     * 返回路由定义流。实现要点：
     *  - 如果缓存已命中，直接返回缓存内容（快速）
     *  - 缓存未命中，则在 boundedElastic 线程池发起阻塞的 Nacos 调用并解析，解析结果写回缓存
     */
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        Object raw = cache.getIfPresent(CACHE_KEY);
        if (raw instanceof List) {
            @SuppressWarnings("unchecked")
            List<RouteDefinition> list = (List<RouteDefinition>) raw;
            log.debug("从缓存命中路由，共 {} 条", list.size());
            return Flux.fromIterable(list);
        }

        // 缓存未命中：异步在 boundedElastic 线程池读取并解析 Nacos 配置，返回 Flux
        return Mono.fromCallable(this::loadFromNacosAndCache)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                // 防止消费者一直等待（防御性超时，实际不常触发）
                .timeout(Duration.ofSeconds(Math.max(5, timeoutMillis / 1000)));
    }

    /**
     * 从 Nacos 拉取配置并解析，成功后写缓存并返回解析后的路由列表
     */
    private List<RouteDefinition> loadFromNacosAndCache() {
        try {
            log.debug("从 Nacos 读取路由配置，dataId={}, group={}", dataId, group);
            String config = nacosConfigManager.getConfigService().getConfig(dataId, group, timeoutMillis);
            if (!StringUtils.hasText(config)) {
                List<RouteDefinition> local = loadFromClasspath(dataId);
                if (!local.isEmpty()) {
                    cache.put(CACHE_KEY, local);
                    log.info("[Nacos Route] 使用本地 classpath 路由，数量={}", local.size());
                    return local;
                }
                log.warn("[Nacos Route] config is empty for dataId={}, group={}", dataId, group);
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

    /**
     * 将 YAML 内容解析为 RouteDefinition 列表
     * 支持常见的 spring.cloud.gateway.routes 两种写法：
     *  - routes: - id: ... uri: ... predicates: - Path=/foo
     *  - predicates 的元素可能是字符串或 Map（如 Path=/foo 或 Path: /foo）
     */
    @SuppressWarnings("unchecked")
    private List<RouteDefinition> parseYamlToRoutes(String ymlContent) {
        List<RouteDefinition> result = new ArrayList<>();
        try {
            Object loaded = yaml.load(ymlContent);
            if (!(loaded instanceof Map)) {
                log.warn("路由配置 yml 顶层结构不是 Map，忽略");
                return result;
            }

            Map<String, Object> root = (Map<String, Object>) loaded;
            Object springObj = root.get("spring");
            if (!(springObj instanceof Map)) {
                log.warn("路由配置缺少 spring 节点或格式不合法");
                return result;
            }
            Map<String, Object> spring = (Map<String, Object>) springObj;

            Object cloudObj = spring.get("cloud");
            if (!(cloudObj instanceof Map)) {
                log.warn("路由配置缺少 spring.cloud 节点或格式不合法");
                return result;
            }
            Map<String, Object> cloud = (Map<String, Object>) cloudObj;

            Object gatewayObj = cloud.get("gateway");
            if (!(gatewayObj instanceof Map)) {
                log.warn("路由配置缺少 spring.cloud.gateway 节点或格式不合法");
                return result;
            }
            Map<String, Object> gateway = (Map<String, Object>) gatewayObj;

            Object routesObj = gateway.get("routes");
            if (!(routesObj instanceof List)) {
                log.warn("spring.cloud.gateway.routes 缺失或不是列表");
                return result;
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) routesObj;
            for (Map<String, Object> r : routes) {
                try {
                    RouteDefinition rd = mapToRouteDefinition(r);
                    if (rd != null && StringUtils.hasText(rd.getId())) {
                        result.add(rd);
                    } else {
                        log.warn("解析到无效路由（缺少 id），已跳过：{}", r);
                    }
                } catch (Exception ex) {
                    log.error("解析单个路由定义失败，跳过该路由", ex);
                }
            }

        } catch (Exception e) {
            log.error("解析路由 YML 失败", e);
        }
        return result;
    }

    /**
     * 将单个 route map 转为 RouteDefinition
     */
    @SuppressWarnings("unchecked")
    private RouteDefinition mapToRouteDefinition(Map<String, Object> map) {
        RouteDefinition rd = new RouteDefinition();

        // id
        Object id = map.get("id");
        if (id == null) return null;
        rd.setId(String.valueOf(id));

        // uri
        Object uri = map.get("uri");
        if (uri != null && StringUtils.hasText(String.valueOf(uri))) {
            rd.setUri(URI.create(String.valueOf(uri)));
        }

        // predicates
        Object preds = map.get("predicates");
        if (preds instanceof List) {
            List<PredicateDefinition> pdList = parsePredicates((List<?>) preds);
            rd.setPredicates(pdList);
        }

        // filters
        Object filters = map.get("filters");
        if (filters instanceof List) {
            List<FilterDefinition> fdList = parseFilters((List<?>) filters);
            rd.setFilters(fdList);
        }

        // metadata
        Object metadata = map.get("metadata");
        if (metadata instanceof Map) {
            rd.setMetadata((Map<String, Object>) metadata);
        }

        // order
        Object order = map.get("order");
        if (order instanceof Number) {
            rd.setOrder(((Number) order).intValue());
        } else if (order != null) {
            try {
                rd.setOrder(Integer.parseInt(String.valueOf(order)));
            } catch (NumberFormatException ignored) {
            }
        }

        return rd;
    }

    /**
     * 解析 predicates 列表，支持字符串和 Map 两种形式
     */
    private List<PredicateDefinition> parsePredicates(List<?> list) {
        return list.stream()
                .map(this::singlePredicateToDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private PredicateDefinition singlePredicateToDefinition(Object obj) {
        PredicateDefinition pd = new PredicateDefinition();
        if (obj instanceof String s) {
            // 例如 "Path=/api/**"
            String[] parts = s.split("=", 2);
            pd.setName(parts[0].trim());
            if (parts.length > 1) {
                pd.setArgs(Collections.singletonMap("_genkey_0", parts[1].trim()));
            }
            return pd;
        } else if (obj instanceof Map<?, ?> map) {
            // 例如 { Path: /api/** } 或 { Path: [ "/api/**", "GET" ] }
            if (map.isEmpty()) return null;
            Map.Entry<?, ?> e = map.entrySet().iterator().next();
            pd.setName(String.valueOf(e.getKey()));
            Object val = e.getValue();
            if (val instanceof String) {
                pd.setArgs(Collections.singletonMap("_genkey_0", String.valueOf(val)));
            } else if (val instanceof List<?> argList) {
                Map<String, String> args = new LinkedHashMap<>();
                for (int i = 0; i < argList.size(); i++) {
                    args.put("_genkey_" + i, String.valueOf(argList.get(i)));
                }
                pd.setArgs(args);
            }
            return pd;
        }
        return null;
    }

    /**
     * 解析 filters 列表（方式同 predicates）
     */
    private List<FilterDefinition> parseFilters(List<?> list) {
        return list.stream()
                .map(this::singleFilterToDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private FilterDefinition singleFilterToDefinition(Object obj) {
        FilterDefinition fd = new FilterDefinition();
        if (obj instanceof String) {
            String s = (String) obj;
            String[] parts = s.split("=", 2);
            fd.setName(parts[0].trim());
            if (parts.length > 1) {
                fd.setArgs(Collections.singletonMap("_genkey_0", parts[1].trim()));
            }
            return fd;
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.isEmpty()) return null;
            Map.Entry<?, ?> e = map.entrySet().iterator().next();
            fd.setName(String.valueOf(e.getKey()));
            Object val = e.getValue();
            if (val instanceof String) {
                fd.setArgs(Collections.singletonMap("_genkey_0", String.valueOf(val)));
            } else if (val instanceof List) {
                List<?> argList = (List<?>) val;
                Map<String, String> args = new LinkedHashMap<>();
                for (int i = 0; i < argList.size(); i++) {
                    args.put("_genkey_" + i, String.valueOf(argList.get(i)));
                }
                fd.setArgs(args);
            }
            return fd;
        }
        return null;
    }

    /**
     * 向 Nacos 注册配置变更监听器，变更时更新本地缓存（最终一致性）
     */
    private void registerNacosListener() {
        if (!listenerRegistered.compareAndSet(false, true)) {
            return;
        }
        try {
            nacosConfigManager.getConfigService().addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return listenerExecutor;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("[Nacos Route] config changed, refreshing cache, dataId={}, group={}", dataId, group);
                    try {
                        List<RouteDefinition> list;
                        if (!StringUtils.hasText(configInfo)) {
                            list = loadFromClasspath(dataId);
                        } else {
                            list = parseYamlToRoutes(configInfo);
                        }
                        cache.put(CACHE_KEY, list);
                        log.info("[Nacos Route] cache refreshed, total routes = {}", list.size());
                    } catch (Exception e) {
                        log.error("[Nacos Route] refresh cache failed", e);
                    }
                }
            });
            log.debug("已为 Nacos dataId={} group={} 注册 listener", dataId, group);
        } catch (Exception e) {
            log.error("为 Nacos 注册 listener 失败", e);
        }
    }

    private List<RouteDefinition> loadFromClasspath(String resourceName) {
        try {
            ClassPathResource resource = new ClassPathResource(resourceName);
            if (!resource.exists()) {
                resource = new ClassPathResource("gateway-routes.yml");
                if (!resource.exists()) {
                    return Collections.emptyList();
                }
            }
            String yml = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            if (!StringUtils.hasText(yml)) {
                return Collections.emptyList();
            }
            return parseYamlToRoutes(yml);
        } catch (Exception e) {
            log.error("从 classpath 加载路由失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> route) {
        // 不实现动态保存（如需实现请扩展）
        log.warn("save 不支持（当前实现为只读缓存 + Nacos 驱动）");
        return Mono.empty();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeId) {
        log.warn("delete 不支持（当前实现为只读缓存 + Nacos 驱动）");
        return Mono.empty();
    }
}
