package com.xy.lucky.gateway.lb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 自定义 im-connect 长连接服务的负载均衡
 * <p>
 * 自定义负载均衡实现需要实现 ReactorServiceInstanceLoadBalancer 接口 以及重写choose方法
 * 主类定义：
 * 自定义负载均衡处理类，只针对转发地址为im-connect的请求生效
 *
 * @LoadBalancerClient(value = "im-connect", configuration = {NacosWebsocketClusterChooseRule.class})
 * <p>
 * 配置开启负载均衡
 */
@Slf4j
public class NacosWebsocketClusterChooseRule implements ReactorServiceInstanceLoadBalancer {

    // 用户前缀
    private static final String IM_USER_PREFIX = "IM-USER-";

    // 机器码
    private static final String IM_BROKER = "brokerId";

    // metadata 中的连接数
    private static final String CONNECTION_COUNT = "connection_count";

    // 轮询计数器
    private final AtomicInteger position = new AtomicInteger(0);

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public NacosWebsocketClusterChooseRule(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, ReactiveRedisTemplate<String, Object> reactiveRedisTemplate) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        RequestDataContext context = (RequestDataContext) request.getContext();
        String rawQuery = context.getClientRequest().getUrl().getRawQuery();
        String uid = extractQueryParam(rawQuery, "uid");

        if (uid == null) {
            log.error("建立连接失败：缺少 uid 参数, url={}", rawQuery);
            return Mono.just(new EmptyResponse());
        }

        log.debug("长连接负载均衡：uid={}, url={}", uid, rawQuery);

        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().flatMap(instances -> {
            if (instances.isEmpty()) {
                log.warn("没有可用的 im-connect 实例");
                return Mono.just(new EmptyResponse());
            }

            return getUserBroker(uid).map(brokerId -> {
                if (StringUtils.hasText(brokerId)) {
                    Optional<ServiceInstance> match = instances.stream()
                            .filter(i -> brokerId.equals(i.getMetadata().get(IM_BROKER)))
                            .findFirst();
                    if (match.isPresent()) {
                        return (Response<ServiceInstance>) new DefaultResponse(match.get());
                    }
                }
                return chooseByLeastConnection(instances);
            });
        });
    }

    /**
     * 最少连接数负载均衡算法
     */
    private Response<ServiceInstance> chooseByLeastConnection(List<ServiceInstance> instances) {
        ServiceInstance instance = instances.stream()
                .min(Comparator.comparingInt(this::getConnectionCount))
                .orElse(instances.get(Math.abs(position.incrementAndGet()) % instances.size()));

        log.info("负载均衡结果：已选实例 {}, 当前连接数 {}", instance.getMetadata().get(IM_BROKER), getConnectionCount(instance));
        return new DefaultResponse(instance);
    }

    private Integer getConnectionCount(ServiceInstance instance) {
        try {
            String count = instance.getMetadata().get(CONNECTION_COUNT);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 异步从 Redis 获取用户绑定的 brokerId
     */
    private Mono<String> getUserBroker(String uid) {
        return reactiveRedisTemplate.opsForValue().get(IM_USER_PREFIX + uid)
                .map(userObj -> {
                    if (userObj instanceof LinkedHashMap<?, ?> map) {
                        Object bId = map.get(IM_BROKER);
                        return bId != null ? bId.toString() : "";
                    }
                    return "";
                })
                .defaultIfEmpty("");
    }

    private String extractQueryParam(String query, String param) {
        if (!StringUtils.hasText(query)) return null;
        try {
            for (String p : query.split("&")) {
                String[] kv = p.split("=");
                if (kv.length > 1 && kv[0].equals(param)) {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
