package com.xy.lucky.connect.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.utils.IPAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Netty 端口注册到 Nacos 的统一服务
 * <p>
 * - 使用 Spring Cloud Alibaba 自动装配的 {@link NamingService}
 * - 按照 discovery 配置，将 TCP / WebSocket 端口注册为实例
 */
@Slf4j(topic = LogConstant.Nacos)
@Component
public class NacosRegistrationService {

    private final NamingService namingService;

    private NacosDiscoveryProperties nacosDiscoveryProperties;

    public NacosRegistrationService(NacosServiceManager nacosServiceManager, NacosDiscoveryProperties nacosDiscoveryProperties) {
        this.nacosDiscoveryProperties = nacosDiscoveryProperties;
        this.namingService = nacosServiceManager.getNamingService();
    }

    /**
     * 注册 TCP 端口到 Nacos
     */
    public void registerTcpPorts(List<Integer> ports) {
        registerPorts(ports, "tcp");
    }

    /**
     * 注册 WebSocket 端口到 Nacos
     */
    public void registerWebsocketPorts(List<Integer> ports) {
        registerPorts(ports, "ws");
    }

    private void registerPorts(List<Integer> ports, String nettyType) {
        if (CollectionUtils.isEmpty(ports)) {
            return;
        }
        if (!nacosDiscoveryProperties.isRegisterEnabled()) {
            log.info("Nacos 服务发现未启用，跳过 Netty 端口注册");
            return;
        }

        String serviceName = nacosDiscoveryProperties.getService();
        String groupName = nacosDiscoveryProperties.getGroup();

        // 优先使用 Nacos 配置的 ip，没有则自动探测本机 IPv4
        String ip = nacosDiscoveryProperties.getIp();
        if (!StringUtils.hasText(ip)) {
            ip = IPAddressUtil.getLocalIp4Address();
        }

        String clusterName = nacosDiscoveryProperties.getClusterName();

        List<Instance> instances = new LinkedList<>();

        for (Integer port : ports) {
            if (port == null) {
                continue;
            }

            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setClusterName(clusterName);
            instance.setWeight(1.0);
            instance.setHealthy(true);

            Map<String, String> metadata = new HashMap<>();
            if (nacosDiscoveryProperties.getMetadata() != null) {
                metadata.putAll(nacosDiscoveryProperties.getMetadata());
            }
            metadata.put("netty", nettyType);
            metadata.put("protocol", nettyType);
            instance.setMetadata(metadata);

            instances.add(instance);
        }

        try {
            namingService.batchRegisterInstance(serviceName, groupName, instances);
            log.info("已注册 Netty 端口到 Nacos: service={}, group={}, ip={}, port={}, type={}",
                    serviceName, groupName, ip, ports, nettyType);
        } catch (Exception e) {
            log.error("注册 Netty 端口到 Nacos 失败, port={}, type={}", ports, nettyType, e);
        }
    }
}

