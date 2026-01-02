package com.xy.lucky.quartz.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 集群管理
 * 负责获取分片信息，支持Nacos
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterManager {

    private final DiscoveryClient discoveryClient;

    @Value("${spring.application.name:im-quartz}")
    private String applicationName;

    /**
     * 获取分片信息
     */
    public int[] getShardingInfo() {
        try {
            // 查找服务的实例
            List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);

            if (instances == null || instances.isEmpty()) {
                return new int[]{0, 1};
            }

            // 排序实例以确保所有节点的顺序一致
            List<ServiceInstance> sortedInstances = instances.stream()
                    .sorted(Comparator.comparing(instance -> instance.getHost() + ":" + instance.getPort()))
                    .toList();

            // 获取当前实例IP
            String currentIp = InetAddress.getLocalHost().getHostAddress();

            for (int i = 0; i < sortedInstances.size(); i++) {
                ServiceInstance instance = sortedInstances.get(i);
                // 简单的IP匹配，生产环境建议使用InstanceId或Registration
                if (instance.getHost().equals(currentIp)) {
                    return new int[]{i, sortedInstances.size()};
                }
            }

            return new int[]{0, 1};

        } catch (Exception e) {
            log.warn("Failed to get cluster info, defaulting to 1/1", e);
            return new int[]{0, 1};
        }
    }

    /**
     * 获取所有实例
     */
    public List<ServiceInstance> getInstances() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(applicationName);
            if (instances == null) return List.of();
            return instances.stream()
                    .sorted(Comparator.comparing(instance -> instance.getHost() + ":" + instance.getPort()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get instances", e);
            return List.of();
        }
    }
}
