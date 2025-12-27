package com.xy.lucky.grpc.client;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.lucky.grpc.client.annotation.GrpcCall;
import com.xy.lucky.grpc.core.generic.GenericRequest;
import com.xy.lucky.grpc.core.generic.GenericResponse;
import com.xy.lucky.grpc.core.generic.GenericServiceGrpc;
import com.xy.lucky.grpc.core.serialize.Serializer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Generic gRPC Invocation Handler / 通用gRPC调用处理器
 * <p>
 * This handler implements Java's dynamic proxy mechanism to provide a generic way
 * to invoke gRPC services. It supports service discovery via Nacos and automatic
 * load balancing.
 * <p>
 * 该处理器实现了Java的动态代理机制，提供了一种通用的方式来调用gRPC服务。
 * 它支持通过Nacos进行服务发现和自动负载均衡。
 */
public class GenericGrpcInvocationHandler implements InvocationHandler, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(GenericGrpcInvocationHandler.class);
    private static final String GRPC_PORT_KEY = "gRPC_port";
    private static final String DEFAULT_CONTENT_TYPE = "application/json"; // 假设常见，复用常量

    // Default timeout in milliseconds / 默认超时时间(毫秒)
    private final long defaultTimeoutMs;
    // Default host / 默认主机地址
    private final String defaultHost;
    // Default port / 默认端口号
    private final int defaultPort;
    // Random number generator for load balancing / 用于负载均衡的随机数生成器
    private final Random random = new Random();
    // Cache for service instances / 服务实例缓存
    private final AtomicReference<List<Instance>> instancesCache = new AtomicReference<>(List.of());
    // Read-write lock for thread safety / 用于线程安全的读写锁
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    // Spring Bean factory / Spring Bean工厂
    private final BeanFactory beanFactory;
    // Service name for discovery / 用于服务发现的服务名称
    private final String serviceName;

    // gRPC channel / gRPC通道
    private volatile ManagedChannel channel;
    // Initialization status / 初始化状态
    private volatile boolean initialized = false;
    // Serializer for data transmission / 用于数据传输的序列化器
    private volatile Serializer serializer;
    // Nacos service manager / Nacos服务管理器
    private NacosServiceManager nacosServiceManager;
    // Nacos discovery properties / Nacos发现属性
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    /**
     * Constructor for GenericGrpcInvocationHandler
     * GenericGrpcInvocationHandler的构造函数
     *
     * @param beanFactory      Spring Bean factory / Spring Bean工厂
     * @param defaultAddress   Default service address / 默认服务地址
     * @param serializer       Data serializer / 数据序列化器
     * @param defaultTimeoutMs Default timeout in milliseconds / 默认超时时间(毫秒)
     * @param serviceName      Service name for discovery / 用于服务发现的服务名称
     */
    public GenericGrpcInvocationHandler(
            BeanFactory beanFactory,
            String defaultAddress,
            Serializer serializer,
            long defaultTimeoutMs,
            String serviceName) {
        this.beanFactory = beanFactory;
        this.serializer = serializer;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.serviceName = serviceName;
        String[] parsed = parseStaticAddress(defaultAddress);
        this.defaultHost = parsed[0];
        this.defaultPort = Integer.parseInt(parsed[1]);
    }

    /**
     * Method invocation handler for gRPC calls
     * gRPC调用的方法执行处理器
     *
     * @param proxy  Proxy object / 代理对象
     * @param method Method being invoked / 被调用的方法
     * @param args   Method arguments / 方法参数
     * @return Method execution result / 方法执行结果
     * @throws Throwable Any exception that might occur / 可能发生的任何异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle methods defined in Object class / 处理Object类中定义的方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // Initialize if not already done / 如果尚未初始化则进行初始化
        if (!initialized) {
            init();
        }

        // Get gRPC call annotation / 获取gRPC调用注解
        GrpcCall ann = method.getAnnotation(GrpcCall.class);
        if (ann == null) {
            throw new IllegalStateException("method must be annotated with @GrpcCall");
            // 方法必须使用@GrpcCall注解
        }

        // Extract URL and timeout from annotation / 从注解中提取URL和超时设置
        String url = ann.value();
        long timeout = ann.timeoutMs() > 0 ? ann.timeoutMs() : defaultTimeoutMs;

        if (log.isDebugEnabled()) {
            log.debug("Preparing to call gRPC method : url={}, timeout={}ms", url, timeout);
            // 准备调用gRPC方法
        }

        // Get return type / 获取返回类型
        Class<?> returnType = method.getReturnType();

        // Serialize method arguments / 序列化方法参数
        byte[] payload = (args == null || args.length == 0) ? new byte[0] : serializer.serialize(args.length == 1 ? args[0] : args);

        // Prepare metadata / 准备元数据
        Map<String, String> metadata = new HashMap<>(2); // 固定大小，减少扩容
        // Fixed size to reduce expansion
        metadata.put("content-type", serializer.contentType() != null ? serializer.contentType() : DEFAULT_CONTENT_TYPE);
        metadata.put("return-class", returnType.getName());

        // Build generic request / 构建通用请求
        GenericRequest req = GenericRequest.newBuilder()
                .setUrl(url)
                .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
                .putAllMetadata(metadata)
                .build();

        // Acquire read lock / 获取读锁
        lock.readLock().lock();
        try {
            // Check if channel is valid / 检查通道是否有效
            if (channel == null || channel.isShutdown()) {
                // Upgrade to write lock / 升级到写锁
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Double check after acquiring write lock / 获取写锁后再次检查
                    if (channel == null || channel.isShutdown()) {
                        refreshChannel();
                    }
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }

            // Create gRPC stub / 创建gRPC存根
            GenericServiceGrpc.GenericServiceBlockingStub stub = GenericServiceGrpc.newBlockingStub(channel);

            // Make gRPC call with or without timeout / 带或不带超时地进行gRPC调用
            GenericResponse resp = (timeout > 0) ? stub.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS).call(req) : stub.call(req);

            // Handle error response / 处理错误响应
            if (resp.getCode() != 0) {
                throw new RuntimeException("grpc call failed: " + resp.getMessage());
                // gRPC调用失败
            }

            // Handle void return or empty response / 处理void返回或空响应
            if (returnType == Void.TYPE || resp.getPayload().isEmpty()) {
                return null;
            }

            // Deserialize response / 反序列化响应
            String returnClassName = resp.getMetadataOrDefault("return-class", returnType.getName());
            Class<?> rt = Class.forName(returnClassName);
            return serializer.deserialize(resp.getPayload().toByteArray(), rt);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Initialize the handler
     * 初始化处理器
     */
    private void init() {
        lock.writeLock().lock();
        try {
            if (initialized) return;

            // Initialize Serializer / 初始化序列化器
            if (serializer == null && beanFactory instanceof ListableBeanFactory) {
                try {
                    serializer = ((ListableBeanFactory) beanFactory).getBean(Serializer.class);
                } catch (BeansException ignored) {
                }
            }
            if (serializer == null) {
                serializer = createDefaultJsonSerializer();
            }

            // Initialize Nacos components / 初始化Nacos组件
            if (beanFactory instanceof ListableBeanFactory) {
                ListableBeanFactory listable = (ListableBeanFactory) beanFactory;
                nacosServiceManager = resolveBean(listable, new String[]{
                        "com.alibaba.cloud.nacos.NacosServiceManager",
                        "com.alibaba.nacos.client.naming.NacosNamingService"
                }, NacosServiceManager.class);
                nacosDiscoveryProperties = resolveBean(listable, new String[]{
                        "com.alibaba.cloud.nacos.NacosDiscoveryProperties"
                }, NacosDiscoveryProperties.class);
            }

            // Subscribe to Nacos if available / 如果可用则订阅Nacos
            if (nacosServiceManager != null && serviceName != null && !serviceName.isEmpty()) {
                subscribeToNacos();
            }

            refreshChannel();
            initialized = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Resolve bean from bean factory
     * 从Bean工厂解析Bean
     *
     * @param listable   Listable bean factory / 可列举的Bean工厂
     * @param classNames Class names to look for / 要查找的类名
     * @param type       Expected bean type / 期望的Bean类型
     * @param <T>        Generic type / 泛型类型
     * @return Resolved bean or null / 解析的Bean或null
     */
    private <T> T resolveBean(ListableBeanFactory listable, String[] classNames, Class<T> type) {
        for (String fqcn : classNames) {
            try {
                Class<?> cls = Class.forName(fqcn);
                String[] names = listable.getBeanNamesForType(cls);
                if (names.length > 0) {
                    return type.cast(listable.getBean(names[0]));
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Subscribe to Nacos service discovery events
     * 订阅Nacos服务发现事件
     */
    private void subscribeToNacos() {
        try {
            NamingService namingService = nacosServiceManager.getNamingService();
            namingService.subscribe(serviceName, event -> {
                if (event instanceof NamingEvent) {
                    lock.writeLock().lock();
                    try {
                        instancesCache.set(((NamingEvent) event).getInstances());
                        refreshChannel();
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Nacos subscribe failed, fallback to manual refresh", e);
            // Nacos订阅失败，回退到手动刷新
        }
    }

    /**
     * Refresh gRPC channel based on service instances
     * 根据服务实例刷新gRPC通道
     */
    private void refreshChannel() {
        List<Instance> instances = instancesCache.get();
        if (instances.isEmpty() && nacosServiceManager != null) {
            try {
                instances = nacosServiceManager.getNamingService().getAllInstances(serviceName);
                instancesCache.set(instances);
            } catch (Exception e) {
                log.warn("Nacos getAllInstances failed", e);
                // Nacos获取所有实例失败
            }
        }

        String host = defaultHost;
        int port = defaultPort;

        if (!instances.isEmpty()) {
            Instance inst = instances.get(random.nextInt(instances.size()));
            host = inst.getIp();
            String portStr = inst.getMetadata().getOrDefault(GRPC_PORT_KEY, "9090");
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ignored) {
                port = defaultPort;
            }
        }

        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }

        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .idleTimeout(5, TimeUnit.MINUTES) // GRPC 内部优化空闲连接
                // GRPC internal optimization for idle connections
                .build();
    }

    /**
     * Create a default JSON serializer
     * 创建默认的JSON序列化器
     *
     * @return Serializer instance / 序列化器实例
     */
    // create a simple default Json serializer that tries to use Jackson if present
    private Serializer createDefaultJsonSerializer() {
        // attempt to use Jackson if available
        try {
            Class<?> omClass = Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            Object mapper = omClass.getDeclaredConstructor().newInstance();
            Method writeMethod = omClass.getMethod("writeValueAsBytes", Object.class);
            Method readMethod = omClass.getMethod("readValue", byte[].class, Class.class);

            return new Serializer() {
                @Override
                public byte[] serialize(Object obj) {
                    try {
                        return (byte[]) writeMethod.invoke(mapper, obj);
                    } catch (Throwable t) {
                        throw new RuntimeException("Jackson serialize failed", t);
                        // Jackson序列化失败
                    }
                }

                @Override
                public <R> R deserialize(byte[] data, Class<R> clazz) {
                    try {
                        return (R) readMethod.invoke(mapper, data, clazz);
                    } catch (Throwable t) {
                        throw new RuntimeException("Jackson deserialize failed", t);
                        // Jackson反序列化失败
                    }
                }

                @Override
                public Object[] deserializeToArray(byte[] bytes, Class<?>[] paramTypes) throws Exception {
                    return new Object[0];
                }

                @Override
                public String contentType() {
                    return "application/json";
                }
            };
        } catch (Throwable ex) {
            // Jackson not present — fallback naive serializer
            // Jackson不存在 - 回退到简单序列化器
            log.warn(" Jackson not found, using default serializer ", ex);
            // Jackson未找到，使用默认序列化器
            return new Serializer() {
                @Override
                public byte[] serialize(Object obj) {
                    if (obj == null) return new byte[0];
                    try {
                        return obj.toString().getBytes("UTF-8");
                    } catch (Exception e) {
                        return new byte[0];
                    }
                }

                @Override
                public <R> R deserialize(byte[] data, Class<R> clazz) {
                    // best-effort: only support String or Void
                    // 最大努力: 仅支持String或Void
                    if (clazz == String.class) {
                        try {
                            return (R) new String(data, "UTF-8");
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }

                @Override
                public Object[] deserializeToArray(byte[] bytes, Class<?>[] paramTypes) throws Exception {
                    return new Object[0];
                }

                @Override
                public String contentType() {
                    return "text/plain";
                }
            };
        }
    }

    /**
     * Parse static address string
     * 解析静态地址字符串
     *
     * @param defaultAddress Address string to parse / 要解析的地址字符串
     * @return Array containing host and port / 包含主机和端口的数组
     */
    private String[] parseStaticAddress(String defaultAddress) {
        String staticHost = "localhost";
        String staticPort = "9090";
        if (defaultAddress != null && !defaultAddress.isEmpty()) {
            String a = defaultAddress.trim();
            if (a.startsWith("static://")) {
                a = a.substring("static://".length());
            }
            if (a.contains(":")) {
                String[] sp = a.split(":");
                staticHost = sp[0];
                staticPort = sp[1];
            } else {
                staticHost = a;
            }
        }
        log.debug("Parsing static address : host={}, port={}", staticHost, staticPort);
        // 解析静态地址
        return new String[]{staticHost, staticPort};
    }

    /**
     * Gracefully shutdown the channel
     * 优雅地关闭通道
     */
    @Override
    public void close() throws Exception {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }
    }
}