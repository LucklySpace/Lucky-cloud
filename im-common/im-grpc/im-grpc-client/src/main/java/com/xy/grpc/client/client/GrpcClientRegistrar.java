package com.xy.grpc.client.client;


import com.xy.grpc.client.annotation.GrpcClient;
import com.xy.grpc.core.serialize.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * Registrar: scan for @GrpcClient interfaces and register GrpcClientFactoryBean for each.
 * This version does NOT require YAML for serializer / nacos class names.
 */
public class GrpcClientRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientRegistrar.class);

    private Environment env;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        log.debug("开始注册gRPC客户端Bean定义");

        Map<String, Object> attrs = importingClassMetadata.getAnnotationAttributes("com.xy.grpc.generic.annotation.EnableGrpcClients");
        String[] basePkgs = (attrs != null && attrs.get("basePackages") != null) ? (String[]) attrs.get("basePackages") : new String[0];

        String defaultBase = null;
        try {
            defaultBase = ClassUtils.getPackageName(importingClassMetadata.getClassName());
        } catch (Exception ignored) {
            log.warn("无法获取默认包名", ignored);
        }

        if (basePkgs == null || basePkgs.length == 0) {
            if (StringUtils.hasText(defaultBase)) {
                basePkgs = new String[]{defaultBase};
            } else {
                basePkgs = new String[]{"com"};
            }
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false) {
                    @Override
                    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                        // accept interfaces too
                        return beanDefinition.getMetadata().isIndependent();
                    }
                };

        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(GrpcClient.class));

        for (String base : basePkgs) {
            if (!StringUtils.hasText(base)) {
                log.debug("跳过空的基础包路径");
                continue;
            }

            log.info("[GrpcClientsRegistrar] scanning base: {}", base);
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(base);
            log.info("[GrpcClientsRegistrar] found: {} candidates in {}", candidates.size(), base);

            for (BeanDefinition bd : candidates) {
                try {
                    String className = bd.getBeanClassName();
                    if (className == null) {
                        log.warn("Bean定义中未找到类名");
                        continue;
                    }

                    log.debug("[GrpcClientsRegistrar] candidate className={}", className);
                    Class<?> clazz = Class.forName(className);

                    GrpcClient ann = clazz.getAnnotation(GrpcClient.class);
                    String clientName = null;
                    if (ann != null) {
                        clientName = StringUtils.hasText(ann.name()) ? ann.name() :
                                (StringUtils.hasText(ann.value()) ? ann.value() : null);
                    }
                    if (!StringUtils.hasText(clientName)) {
                        clientName = clazz.getSimpleName();
                    }

                    String propPrefix = "grpc.client." + clientName;
                    // address/timeout optional; if not present, FactoryBean will fallback to default static
                    String address = env != null ?
                            env.getProperty(propPrefix + ".address",
                                    env.getProperty(propPrefix + ".url", "localhost:9090")) :
                            "localhost:9090";

                    long timeout = 0L;
                    try {
                        String timeoutStr = env != null ? env.getProperty(propPrefix + ".timeout", "0") : "0";
                        timeout = Long.parseLong(timeoutStr);
                        log.debug("读取到超时配置: {} = {}ms", propPrefix + ".timeout", timeout);
                    } catch (NumberFormatException ignored) {
                        log.warn("无效的超时配置，使用默认值0");
                    }

                    RootBeanDefinition beanDef = new RootBeanDefinition(GrpcClientFactoryBean.class);
                    beanDef.getPropertyValues().add("interfaceType", clazz);
                    beanDef.getPropertyValues().add("address", address);
                    beanDef.getPropertyValues().add("serviceName", clientName);
                    beanDef.getPropertyValues().add("defaultTimeoutMs", timeout);
                    beanDef.getPropertyValues().add("serializer", new JsonSerializer());

                    // NOTE: we do NOT pass serializer / nacos class names via YAML.
                    // FactoryBean will auto-resolve serializer and Nacos components at runtime.

                    beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

                    String beanName = className; // register under full classname
                    if (!registry.containsBeanDefinition(beanName)) {
                        registry.registerBeanDefinition(beanName, beanDef);
                        log.info("[GrpcClientsRegistrar] registered GrpcClient bean: {} -> {}", beanName, address);
                    } else {
                        log.warn("[GrpcClientsRegistrar] bean {} already exists, skipping", beanName);
                    }

                } catch (Throwable ex) {
                    log.error("注册gRPC客户端时发生错误: {}", bd.getBeanClassName(), ex);
                    throw new RuntimeException("register grpc client error for " + bd.getBeanClassName(), ex);
                }
            }
        }

        log.info("gRPC客户端Bean定义注册完成");
    }
}