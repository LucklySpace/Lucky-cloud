package com.xy.grpc.client;

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
 * gRPC Client Factory Bean / gRPC客户端工厂Bean
 * <p>
 * This factory bean is responsible for creating proxy instances for gRPC client interfaces.
 * It integrates with Spring's bean lifecycle and provides configuration options for gRPC clients.
 * <p>
 * 该工厂Bean负责为gRPC客户端接口创建代理实例。
 * 它与Spring的Bean生命周期集成，并提供gRPC客户端的配置选项。
 */
@SuppressWarnings("unchecked")
public class GrpcClientFactoryBean<T> implements FactoryBean<T>, InitializingBean, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(GrpcClientFactoryBean.class);

    // Client interface type / 客户端接口类型
    private Class<T> interfaceType;
    // Service address / 服务地址
    private String address;
    // Service name for discovery / 用于服务发现的服务名称
    private String serviceName;
    // Default timeout in milliseconds / 默认超时时间(毫秒)
    private long defaultTimeoutMs = 0L;
    // Serializer for data transmission / 用于数据传输的序列化器
    private Serializer serializer;
    // Proxy instance / 代理实例
    private T proxy;
    // Spring Bean factory / Spring Bean工厂
    private BeanFactory beanFactory;

    // ----- setters used by Registrar -----
    // ----- 由注册器使用的setter方法 -----

    /**
     * Set the client interface type
     * 设置客户端接口类型
     *
     * @param interfaceType Client interface type / 客户端接口类型
     */
    public void setInterfaceType(Class<T> interfaceType) {
        this.interfaceType = interfaceType;
    }

    /**
     * Set the service address
     * 设置服务地址
     *
     * @param address Service address / 服务地址
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Set the service name for discovery
     * 设置用于服务发现的服务名称
     *
     * @param serviceName Service name / 服务名称
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Set the default timeout in milliseconds
     * 设置默认超时时间(毫秒)
     *
     * @param defaultTimeoutMs Default timeout / 默认超时时间
     */
    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    // optional setters if autowired by type
    // 如果按类型自动装配的可选setter方法

    /**
     * Set the serializer (optional)
     * 设置序列化器(可选)
     *
     * @param serializer Data serializer / 数据序列化器
     */
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Set the Spring Bean factory
     * 设置Spring Bean工厂
     *
     * @param beanFactory Bean factory / Bean工厂
     * @throws BeansException Bean exception / Bean异常
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        log.trace("Setting BeanFactory: {}", beanFactory);
        // 设置BeanFactory
    }

    /**
     * Initialize the factory bean after properties are set
     * 在属性设置完成后初始化工厂Bean
     *
     * @throws Exception Any initialization exception / 任何初始化异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(interfaceType, "interfaceType must not be null");
        // interfaceType不能为空
        log.debug("Initialize GrpcClientFactoryBean: interfaceType={}", interfaceType.getName());
        // 初始化GrpcClientFactoryBean

        // create invocation handler - use the new constructor with Nacos dependencies
        // 创建调用处理器 - 使用带有Nacos依赖的新构造函数
        GenericGrpcInvocationHandler handler = new GenericGrpcInvocationHandler(
                beanFactory,
                address,
                serializer,
                defaultTimeoutMs,
                serviceName);
        log.info("[GrpcClientFactoryBean] invocation handler created for {}", interfaceType.getName());
        // 为...创建了调用处理器
        this.proxy = (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType}, handler);

        log.info("[GrpcClientFactoryBean] proxy created for {}", interfaceType.getName());
        // 为...创建了代理
    }

    /**
     * Get the proxy object
     * 获取代理对象
     *
     * @return Proxy instance / 代理实例
     */
    @Override
    public T getObject() {
        log.trace("Get proxy bean: {}", interfaceType.getName());
        // 获取代理Bean
        return proxy;
    }

    /**
     * Get the type of the proxy object
     * 获取代理对象的类型
     *
     * @return Object type / 对象类型
     */
    @Override
    public Class<?> getObjectType() {
        return this.interfaceType;
    }

    /**
     * Check if the bean is singleton
     * 检查Bean是否为单例
     *
     * @return true if singleton, false otherwise / 如果是单例则返回true，否则返回false
     */
    @Override
    public boolean isSingleton() {
        return true;
    }
}