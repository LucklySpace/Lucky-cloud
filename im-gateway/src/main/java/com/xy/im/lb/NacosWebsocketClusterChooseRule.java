package com.xy.im.lb;

import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SelectedInstanceCallback;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 自定义负载均衡实现需要实现 ReactorServiceInstanceLoadBalancer 接口 以及重写choose方法
 * 主类定义：
 * // 自定义负载均衡处理类，只针对转发地址为im-connect的请求生效
 *
 * @LoadBalancerClient(value = "im-connect", configuration = {NacosWebsocketClusterChooseRule.class})
 *
 * 配置开启负载均衡
 */
@Slf4j
public class NacosWebsocketClusterChooseRule implements ReactorServiceInstanceLoadBalancer {

    private static final String IMUSERPREFIX = "IM-USER-";
    private static final String IMBROKER = "broker_id";
    private final AtomicInteger position = new AtomicInteger(0); // 轮询计数器
    private static final String CONNECTION_COUNT = "connection_count"; // metadata 中的连接数

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public NacosWebsocketClusterChooseRule(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    @SneakyThrows
    public Mono<Response<ServiceInstance>> choose(Request request) {
        String rawQuery = ((RequestDataContext) request.getContext()).getClientRequest().getUrl().getRawQuery();
        String uid = extractQueryParam(rawQuery, "uid");

        if (uid == null) {
            log.error("建立连接失败，缺少字段uid，url:{}", rawQuery);
            return Mono.just(new EmptyResponse());
        }

        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances, uid));
    }

    /**
     * 根据用户ID获取服务器实例
     *
     * @param supplier
     * @param serviceInstances
     * @param uid
     * @return
     */
    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier, List<ServiceInstance> serviceInstances, String uid) {
        Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances, uid);
        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
        }
        return serviceInstanceResponse;
    }

    /**
     * 获取用户服务器实例
     *
     * @param instances 服务器实例列表
     * @param uid       用户ID
     * @return 服务器实例
     */
//    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, String uid) {
//        if (instances.isEmpty()) {
//            log.warn("没有可用的服务器实例");
//            return new EmptyResponse();
//        }
//        // 根据用户ID选择服务器实例
//        String brokerId = getUserBroker(uid);
//        if (StringUtils.hasText(brokerId)) {
//            log.debug("当前用户 {} 的 brokerId 为：{}", uid, brokerId);
//        }
//        return instances.stream()
//                .filter(instance -> StringUtils.hasText(brokerId) && brokerId.equals(instance.getMetadata().get(IMBROKER)))
//                .findFirst()
//                .map(DefaultResponse::new)
//                .orElseGet(() -> {
//                    log.info("没有找到匹配的 brokerId，随机分配服务器实例");
//                    ServiceInstance serviceInstance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
//                    log.info("用户 {} 分配的服务器为：{}", uid, serviceInstance.getMetadata().get(IMBROKER));
//                    return new DefaultResponse(serviceInstance);
//                });
//    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, String uid) {
        if (instances.isEmpty()) {
            log.warn("没有可用的服务器实例");
            return new EmptyResponse();
        }

        // 根据用户ID选择服务器实例
        String brokerId = getUserBroker(uid);
        if (StringUtils.hasText(brokerId)) {
            log.debug("当前用户 {} 的 brokerId 为：{}", uid, brokerId);
            return instances.stream()
                    .filter(instance -> brokerId.equals(instance.getMetadata().get(IMBROKER)))
                    .findFirst()
                    .map(DefaultResponse::new)
                    .orElseGet(() -> (DefaultResponse) chooseByRoundRobin(instances, uid));
        }

        // 如果没有匹配的 brokerId，使用轮询算法选择实例
        return chooseByRoundRobin(instances, uid);
    }

    /**
     * 轮训负载算法
     * 轮询负载均衡（Round-Robin）： 如果用户没有绑定特定的服务器，可以改为轮询而不是随机选择。你可以维护一个轮询计数器，将请求依次分配到不同的服务器实例上。
     * @param instances 实例
     * @param uid 用户id
     * @return
     */
    private Response<ServiceInstance> chooseByRoundRobin(List<ServiceInstance> instances, String uid) {
        // 轮询算法选择实例
        int pos = Math.abs(position.incrementAndGet()) % instances.size();
        ServiceInstance serviceInstance = instances.get(pos);
        log.info("用户 {} 分配的服务器为：{}", uid, serviceInstance.getMetadata().get(IMBROKER));
        return new DefaultResponse(serviceInstance);
    }


    /**
     * 最少连接数算法选择实例
     * @param instances 服务器实例列表
     * @return
     */
    private Response<ServiceInstance> chooseByLeastConnection(List<ServiceInstance> instances) {
        // 找到连接数最少的实例
        ServiceInstance leastConnectionInstance = instances.stream()
                .min(Comparator.comparingInt(this::getConnectionCount))
                .orElseThrow(() -> new IllegalStateException("找不到合适的服务实例"));

        log.info("选择的服务器为：{}，连接数为：{}", leastConnectionInstance.getMetadata().get(IMBROKER), getConnectionCount(leastConnectionInstance));
        return new DefaultResponse(leastConnectionInstance);
    }

    /**
     * 从服务实例的 metadata 中获取连接数
     * @param instance 服务实例
     * @return 连接数
     */
    private int getConnectionCount(ServiceInstance instance) {
        String connectionCountStr = instance.getMetadata().get(CONNECTION_COUNT);
        try {
            return connectionCountStr != null ? Integer.parseInt(connectionCountStr) : Integer.MAX_VALUE;
        } catch (NumberFormatException e) {
            log.error("解析连接数失败: {}", connectionCountStr, e);
            return Integer.MAX_VALUE;  // 如果解析失败，视为最大值，避免选择这个实例
        }
    }

    /**
     * 根据用户ID获取对应的broker
     *
     * @param uid
     * @return
     */
    private String getUserBroker(String uid) {
        try {
            Object userObj = redisTemplate.boundValueOps(IMUSERPREFIX + uid).get();
            return userObj != null ? ((LinkedHashMap<?, ?>) userObj).get(IMBROKER).toString() : null;
        } catch (Exception e) {
            log.error("Redis 获取 broker 出错，UID: {}", uid, e);
            return null;
        }
    }

    /**
     * 从查询参数中提取指定参数的值
     *
     * @param query
     * @param param
     * @return
     * @throws UnsupportedEncodingException
     */
    private String extractQueryParam(String query, String param) throws UnsupportedEncodingException {
        for (String p : query.split("&")) {
            String[] keyValue = p.split("=");
            if (keyValue[0].equals(param)) {
                return URLDecoder.decode(keyValue[1], "UTF-8");
            }
        }
        return null;
    }
}