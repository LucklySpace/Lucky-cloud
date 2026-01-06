package com.xy.lucky.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Selective lazy initialization configuration
 * - Core components (Controllers, Security, Health checks) are eagerly initialized
 * - Non-critical services can be lazily initialized to speed up startup
 */
@Slf4j
@Configuration
public class LazyInitConfig {

    /**
     * Selectively mark beans as lazy based on their type
     * Exclude critical beans like Controllers, Health indicators, etc.
     */
    @Bean
    public static BeanFactoryPostProcessor lazyBeanPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                String lazyInitProperty = beanFactory.resolveEmbeddedValue("${spring.main.lazy-initialization:false}");
                if (!"true".equalsIgnoreCase(lazyInitProperty)) {
                    return; // Only apply if lazy-initialization is enabled
                }

                String[] beanNames = beanFactory.getBeanDefinitionNames();
                int lazyCount = 0;

                for (String beanName : beanNames) {
                    if (beanFactory.containsBeanDefinition(beanName)) {
                        var beanDef = beanFactory.getBeanDefinition(beanName);
                        
                        // Skip beans that should be eagerly initialized
                        String beanClassName = beanDef.getBeanClassName();
                        if (beanClassName != null && shouldBeEager(beanClassName)) {
                            beanDef.setLazyInit(false);
                            continue;
                        }

                        // Mark eligible beans as lazy
                        if (!beanDef.isLazyInit() && beanClassName != null && shouldBeLazy(beanClassName)) {
                            beanDef.setLazyInit(true);
                            lazyCount++;
                        }
                    }
                }

                log.info("Lazy initialization enabled: {} beans marked as lazy", lazyCount);
            }

            private boolean shouldBeEager(String className) {
                // Always eagerly initialize these types
                return className.contains("Controller") ||
                       className.contains("HealthIndicator") ||
                       className.contains("SecurityConfig") ||
                       className.contains("StartupMetrics") ||
                       className.contains("DataSource") ||
                       className.contains("Initializer") ||
                       className.endsWith("Application");
            }

            private boolean shouldBeLazy(String className) {
                // Lazily initialize these types (non-critical services)
                return className.contains("Service") && !className.contains("ServiceImpl") ||
                       className.contains("Util") ||
                       className.contains("Helper") ||
                       className.contains("Validator");
            }
        };
    }
}

