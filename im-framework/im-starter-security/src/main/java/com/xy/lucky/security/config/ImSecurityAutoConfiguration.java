package com.xy.lucky.security.config;

import com.xy.lucky.security.SecurityAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SecurityAuthProperties.class})
public class ImSecurityAutoConfiguration {
}

