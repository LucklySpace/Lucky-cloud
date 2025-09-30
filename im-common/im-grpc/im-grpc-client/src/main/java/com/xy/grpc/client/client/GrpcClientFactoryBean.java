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


@SuppressWarnings("unchecked")
public class GrpcClientFactoryBean<T> implements FactoryBean<T>, InitializingBean, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientFactoryBean.class);

    private Class<T> interfaceType;
    private String address;
    private String serviceName;
    private long defaultTimeoutMs = 0L;
    private Serializer serializer;
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
        log.trace("Setting BeanFactory: {}", beanFactory);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(interfaceType, "interfaceType must not be null");
        log.debug("Initialize GrpcClientFactoryBean: interfaceType={}", interfaceType.getName());

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
        log.trace("Get proxy bean: {}", interfaceType.getName());
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