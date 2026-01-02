package com.xy.lucky.job.config;

import com.xy.lucky.job.core.JobAdminClient;
import com.xy.lucky.job.core.JobExecutorController;
import com.xy.lucky.job.core.JobRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 自动配置类
 *
 * @author lucky
 */
@Configuration
@EnableConfigurationProperties(LuckyJobProperties.class)
@ConditionalOnProperty(prefix = "lucky.job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LuckyJobAutoConfiguration {

    @Bean
    public JobRegistry jobRegistry() {
        return new JobRegistry();
    }

    @Bean
    public JobExecutorController jobExecutorController() {
        return new JobExecutorController();
    }

    @Bean
    public JobAdminClient jobAdminClient(LuckyJobProperties properties, Environment environment) {
        return new JobAdminClient(properties, environment);
    }
}
