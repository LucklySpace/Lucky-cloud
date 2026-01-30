package com.xy.lucky.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewayAuthConfiguration {
}

