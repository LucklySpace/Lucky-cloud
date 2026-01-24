package com.xy.lucky.gateway;

import com.xy.lucky.gateway.lb.NacosWebsocketClusterChooseRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;

/**
 * Lucky-cloud 网关服务启动类
 * 职责：负责全局路由转发、安全校验、灰度发布及流量监控。
 */
@EnableDiscoveryClient
@LoadBalancerClient(value = "im-connect", configuration = {NacosWebsocketClusterChooseRule.class})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ImGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImGatewayApplication.class, args);
    }

}
