package com.xy.lucky.gateway;


import com.xy.lucky.gateway.lb.NacosWebsocketClusterChooseRule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;

@SpringBootApplication
// nacos
@EnableDiscoveryClient
// 自定义负载均衡处理类，只针对转发地址为im-connect的请求生效
@LoadBalancerClient(value = "im-connect", configuration = {NacosWebsocketClusterChooseRule.class})
public class ImGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImGatewayApplication.class, args);
    }

}
