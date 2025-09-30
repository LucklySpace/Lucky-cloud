package com.xy.grpc.client.client;


import com.xy.grpc.core.serialize.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.lang.reflect.Proxy;

/**
 * FactoryBean that attempts to auto-resolve:
 * - Serializer (by type from context or fallback to internal DefaultJsonSerializer)
 * - NacosServiceManager / NacosDiscoveryProperties (by type if present in context)
 * If Nacos is not available, falls back to static address parsing.
 * <p>
 * This implementation does NOT require configuration in YAML for serializer or nacos class names.
 */
@SuppressWarnings("unchecked")
public class GrpcClientFactoryBean<T> implements FactoryBean<T>, InitializingBean, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientFactoryBean.class);

    private Class<T> interfaceType;
    private String address;
    private String serviceName;
    private long defaultTimeoutMs = 0L;

    // optional: container may set these by autowire-by-type
    private Serializer serializer;
    // we keep Nacos objects as Object to avoid compile-time dependency
    private T proxy;
    private BeanFactory beanFactory;

    // ----- setters used by Registrar -----
    public void setInterfaceType(Class<T> interfaceType) {
        this.interfaceType = interfaceType;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    // optional setters if autowired by type
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        log.trace("设置BeanFactory: {}", beanFactory);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(interfaceType, "interfaceType must not be null");
        log.debug("初始化GrpcClientFactoryBean: interfaceType={}", interfaceType.getName());

        // create invocation handler - use the new constructor with Nacos dependencies
        GenericGrpcInvocationHandler handler = new GenericGrpcInvocationHandler(
                beanFactory,
                address,
                serializer,
                defaultTimeoutMs,
                serviceName);
        log.info("[GrpcClientFactoryBean] invocation handler created for {}", interfaceType.getName());
        this.proxy = (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType}, handler);

        log.info("[GrpcClientFactoryBean] proxy created for {}", interfaceType.getName());
    }

    @Override
    public T getObject() {
        log.trace("获取代理对象: {}", interfaceType.getName());
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return this.interfaceType;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}