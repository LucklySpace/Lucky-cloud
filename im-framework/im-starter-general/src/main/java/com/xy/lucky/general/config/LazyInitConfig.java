package com.xy.lucky.general.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


@Slf4j
@Configuration
@Lazy(value = false)
public class LazyInitConfig implements EnvironmentAware {

    private static final String DEFAULT_LAZY_INIT_KEY = "default";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Creates a BeanFactoryPostProcessor that selectively marks beans as lazy
     * based on regex patterns from Nacos configuration.
     *
     * @return BeanFactoryPostProcessor for lazy initialization control
     */
    @Bean
    public BeanFactoryPostProcessor lazyBeanPostProcessor() {
        return new BeanFactoryPostProcessor() {

            private final List<Pattern> compiledPatterns = new ArrayList<>();

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                // Get current application name
                String applicationName = environment.getProperty("spring.application.name", "");

                // Read lazy-init configuration from Nacos
                // im.lazy-init.config is a list, each item has 'id', 'enabled' and 'exclude-patterns'
                List<String> defaultPatterns = new ArrayList<>();
                List<String> servicePatterns = new ArrayList<>();
                Boolean lazyEnabled = null;

                int configIndex = 0;
                while (true) {
                    String idKey = "im.lazy-init.config[" + configIndex + "].id";
                    String configId = environment.getProperty(idKey);

                    if (configId == null) {
                        break;
                    }

                    // Read exclude-patterns for this config item
                    String patternPrefix = "im.lazy-init.config[" + configIndex + "].exclude-patterns";
                    List<String> patterns = readListProperty(patternPrefix);

                    if (DEFAULT_LAZY_INIT_KEY.equals(configId)) {
                        // This is the default/common configuration
                        defaultPatterns.addAll(patterns);
                    } else if (configId.equals(applicationName)) {
                        // This is the service-specific configuration
                        servicePatterns.addAll(patterns);
                        // Read the enabled flag for this service
                        String enabledKey = "im.lazy-init.config[" + configIndex + "].enabled";
                        String enabledValue = environment.getProperty(enabledKey, "false");
                        lazyEnabled = Boolean.parseBoolean(enabledValue);
                    }

                    configIndex++;
                }

                // Check if lazy initialization is enabled for this service
                if (lazyEnabled == null || !lazyEnabled) {
                    log.info("Lazy initialization is disabled for service: {}", applicationName);
                    return;
                }

                // Merge and compile exclude patterns
                List<String> allPatterns = mergePatterns(defaultPatterns, servicePatterns);
                compilePatterns(allPatterns);

                // Process all bean definitions
                String[] beanNames = beanFactory.getBeanDefinitionNames();
                int lazyCount = 0;
                int eagerCount = 0;

                for (String beanName : beanNames) {
                    if (beanFactory.containsBeanDefinition(beanName)) {
                        BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
                        String beanClassName = beanDef.getBeanClassName();

                        if (beanClassName == null) {
                            continue;
                        }

                        // Check if bean should be eagerly initialized
                        if (shouldBeEager(beanClassName)) {
                            beanDef.setLazyInit(false);
                            eagerCount++;
                        } else {
                            // Mark as lazy if not already
                            if (!beanDef.isLazyInit()) {
                                beanDef.setLazyInit(true);
                                lazyCount++;
                            }
                        }
                    }
                }

                log.info("Lazy initialization applied: {} beans marked as lazy, {} beans marked as eager", lazyCount, eagerCount);
            }

            /**
             * Reads a list property from Environment.
             * Supports indexed property format: property[0], property[1], etc.
             *
             * @param propertyPrefix the property prefix (e.g., "lazy-init.common.exclude-patterns")
             * @return list of property values
             */
            private List<String> readListProperty(String propertyPrefix) {
                List<String> result = new ArrayList<>();
                int index = 0;

                while (true) {
                    String key = propertyPrefix + "[" + index + "]";
                    String value = environment.getProperty(key);

                    if (value == null) {
                        break;
                    }

                    if (!value.isBlank()) {
                        result.add(value.trim());
                    }
                    index++;
                }

                return result;
            }

            /**
             * Merges common and service-specific exclude patterns into a single list.
             *
             * @param commonPatterns  common patterns shared across services
             * @param servicePatterns service-specific patterns
             * @return merged list of exclude patterns
             */
            private List<String> mergePatterns(List<String> commonPatterns, List<String> servicePatterns) {
                List<String> merged = new ArrayList<>();

                if (commonPatterns != null && !commonPatterns.isEmpty()) {
                    merged.addAll(commonPatterns);
                }

                if (servicePatterns != null && !servicePatterns.isEmpty()) {
                    merged.addAll(servicePatterns);
                }

                return merged;
            }

            /**
             * Compiles regex patterns from string list.
             * Invalid patterns are logged and skipped.
             *
             * @param patterns list of regex pattern strings
             */
            private void compilePatterns(List<String> patterns) {
                for (String regex : patterns) {
                    if (regex == null || regex.isBlank()) {
                        continue;
                    }

                    try {
                        compiledPatterns.add(Pattern.compile(regex));
                    } catch (PatternSyntaxException e) {
                        log.warn("Invalid regex pattern '{}', skipping: {}", regex, e.getMessage());
                    }
                }
            }

            /**
             * Determines if a bean should be eagerly initialized based on exclude patterns.
             *
             * @param className the fully qualified class name of the bean
             * @return true if the bean matches any exclude pattern (should be eager), false otherwise
             */
            private boolean shouldBeEager(String className) {
                for (Pattern pattern : compiledPatterns) {
                    try {
                        if (pattern.matcher(className).matches()) {
                            return true;
                        }
                    } catch (Exception e) {
                        log.warn("Error matching pattern '{}' against class '{}': {}",
                                pattern.pattern(), className, e.getMessage());
                    }
                }
                return false;
            }
        };
    }
}
