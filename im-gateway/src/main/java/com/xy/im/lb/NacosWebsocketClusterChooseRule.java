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
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 自定义负载均衡实现需要实现 ReactorServiceInstanceLoadBalancer 接口 以及重写choose方法
 * 主类定义：
 * // 自定义负载均衡处理类，只针对转发地址为im-connect的请求生效
 *
 * @LoadBalancerClient(value = "im-connect", configuration = {NacosWebsocketClusterChooseRule.class})
 * <p>
 * 配置开启负载均衡
 */
@Slf4j
public class NacosWebsocketClusterChooseRule implements ReactorServiceInstanceLoadBalancer {

    private static final String IMUSERPREFIX = "IM-USER-"; // 用户前缀

    private static final String IMBROKER = "broker_id"; // 机器码

    private static final String CONNECTION_COUNT = "connection_count"; // metadata 中的连接数

    private final AtomicInteger position = new AtomicInteger(0); // 轮询计数器

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private final RedisTemplate<String, String> redisTemplate;

    public NacosWebsocketClusterChooseRule(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,RedisTemplate<String, String> redisTemplate) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.redisTemplate = redisTemplate;
    }

    @SneakyThrows
    public Mono<Response<ServiceInstance>> choose(Request request) {

        // 获取请求url
        String rawQuery = ((RequestDataContext) request.getContext()).getClientRequest().getUrl().getRawQuery();

        String uid = extractQueryParam(rawQuery, "uid");

        if (uid == null) {
            log.error("建立连接失败，缺少字段uid，url:{}", rawQuery);
            return Mono.just(new EmptyResponse());
        }

        log.debug("当前用户 {} 的请求url为：{}", uid, rawQuery);

        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);

        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances, uid));
    }

    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier, List<ServiceInstance> serviceInstances, String uid) {

        Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances, uid);

        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
        }

        return serviceInstanceResponse;
    }

    /**
     * 获取实例
     * @param instances 实例列表
     * @param uid 用户ID
     * @return 实例
     */
    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, String uid) {

        if (instances.isEmpty()) {
            log.warn("没有可用的服务器实例");
            return new EmptyResponse();
        }

        // 根据用户ID选择服务器实例
        String brokerId = getUserBroker(uid);

        if (StringUtils.hasText(brokerId)) {
            log.debug("当前用户 {} 的 brokerId 为：{}", uid, brokerId);
            // 根据机器码从实例列表选择实例 ，如果没有匹配的 brokerId，使用结合轮询与最小连接数的算法选择实例
            return instances.stream()
                    .filter(instance -> brokerId.equals(instance.getMetadata().get(IMBROKER)))
                    .findFirst()
                    .map(DefaultResponse::new)
                    .orElseGet(() -> (DefaultResponse)chooseByRoundRobinWithLeastConnection(instances, uid));
        }

        // 如果没有匹配的 brokerId，使用结合轮询与最小连接数的算法选择实例
        return chooseByRoundRobinWithLeastConnection(instances, uid);
    }

    /**
     * 轮询与最小连接数结合的负载算法
     * 轮询负载均衡 + 最少连接数选择：每次轮询选择一个实例，且优先选择连接数较少的实例。
     *
     * @param instances 实例列表
     * @param uid       用户ID
     * @return 选择的服务器实例
     */
    private Response<ServiceInstance> chooseByRoundRobinWithLeastConnection(List<ServiceInstance> instances, String uid) {
        // 获取当前的轮询位置
        int pos = Math.abs(position.incrementAndGet()) % instances.size();

        ServiceInstance chosenInstance = instances.get(pos);

        // 获取实例的连接数并比较，选择连接数最少的实例
//        chosenInstance = instances.stream()
//                .min(Comparator.comparingInt(this::getConnectionCount))
//                .orElse(chosenInstance);

        log.info("用户 {} 分配的服务器为：{}，连接数为：{}", uid, chosenInstance.getMetadata().get(IMBROKER), getConnectionCount(chosenInstance));

        return new DefaultResponse(chosenInstance);
    }

    /**
     * 最少连接数算法选择实例
     *
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
     * 获取连接数的最少值
     * @param instance 服务实例
     * @return 连接数
     */
    private Integer getConnectionCount(ServiceInstance instance) {
        String connectionCountStr = instance.getMetadata().get(CONNECTION_COUNT);
        try {
            return connectionCountStr != null ? Integer.parseInt(connectionCountStr) : 0;
        } catch (NumberFormatException e) {
            log.error("解析连接数失败: {}", connectionCountStr, e);
            return Integer.MAX_VALUE;  // 如果解析失败，视为最大值，避免选择这个实例
        }
    }

    /**
     * 根据用户ID获取用户对应的 brokerId
     * @param uid 用户ID
     * @return brokerId
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
     * 解析查询参数
     * @param url url
     * @param param 参数名
     * @return 参数值
     * @throws UnsupportedEncodingException
     */
    private String extractQueryParam(String url, String param) throws UnsupportedEncodingException {
        for (String p : url.split("&")) {
            String[] keyValue = p.split("=");
            if (keyValue[0].equals(param)) {
                return URLDecoder.decode(keyValue[1], "UTF-8");
            }
        }
        return null;
    }
}
