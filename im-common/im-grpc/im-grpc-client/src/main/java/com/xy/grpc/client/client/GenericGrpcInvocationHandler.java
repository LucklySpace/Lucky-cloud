package com.xy.grpc.client.client;


import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.grpc.client.annotation.GrpcCall;
import com.xy.grpc.core.generic.GenericRequest;
import com.xy.grpc.core.generic.GenericResponse;
import com.xy.grpc.core.generic.GenericServiceGrpc;
import com.xy.grpc.core.serialize.Serializer;
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


public class GenericGrpcInvocationHandler implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(GenericGrpcInvocationHandler.class);
    private static final String GRPC_PORT_KEY = "gRPC_port";

    private final long defaultTimeoutMs;
    // 默认地址，当Nacos不可用时使用
    private final String defaultAddress;
    private final Random random = new Random();
    // 使用ThreadLocal存储channel，确保线程安全
    private final ThreadLocal<ManagedChannel> channelThreadLocal = new ThreadLocal<>();
    // Nacos相关依赖（可选）
    private NacosServiceManager nacosServiceManager;
    private NacosDiscoveryProperties nacosDiscoveryProperties;
    private String serviceName;
    private Serializer serializer;
    private BeanFactory beanFactory;

    public GenericGrpcInvocationHandler(
            BeanFactory beanFactory,
            String defaultAddress,
            Serializer serializer,
            long defaultTimeoutMs,
            String serviceName) {
        this.beanFactory = beanFactory;
        this.defaultAddress = defaultAddress;
        this.serializer = serializer;
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.serviceName = serviceName;

        log.debug("创建GenericGrpcInvocationHandler: serviceName={}, defaultAddress={}, defaultTimeoutMs={}",
                serviceName, defaultAddress, defaultTimeoutMs);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            log.trace("调用Object类方法: {}", method.getName());
            return method.invoke(this, args);
        }

        if (nacosServiceManager == null) {
            log.debug("初始化Nacos相关组件");
            init();
        }

        GrpcCall ann = method.getAnnotation(GrpcCall.class);
        if (ann == null) {
            log.error("方法未使用@GrpcCall注解: {}", method.getName());
            throw new IllegalStateException("method must be annotated with @GrpcCall");
        }

        String url = ann.value();
        long timeout = ann.timeoutMs() > 0 ? ann.timeoutMs() : defaultTimeoutMs;

        log.debug("准备调用gRPC方法: url={}, timeout={}ms", url, timeout);

        Class<?> returnType = method.getReturnType();

        byte[] payload = (args == null || args.length == 0) ? new byte[0] : serializer.serialize(args.length == 1 ? args[0] : args);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("content-type", serializer.contentType());
        metadata.put("return-class", returnType.getName());

        GenericRequest req = GenericRequest.newBuilder()
                .setUrl(url)
                .setPayload(com.google.protobuf.ByteString.copyFrom(payload))
                .putAllMetadata(metadata)
                .build();

        // 动态创建Channel
        ManagedChannel channel = createChannel();

        try {
            GenericServiceGrpc.GenericServiceBlockingStub stub = GenericServiceGrpc.newBlockingStub(channel);

            GenericResponse resp;
            if (timeout > 0) {
                log.debug("使用超时设置调用gRPC服务: {}ms", timeout);
                resp = stub.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS).call(req);
            } else {
                log.debug("无超时设置调用gRPC服务");
                resp = stub.call(req);
            }

            if (resp.getCode() != 0) {
                log.error("gRPC调用失败: code={}, message={}", resp.getCode(), resp.getMessage());
                throw new RuntimeException("grpc call failed: " + resp.getMessage());
            }

            if (returnType == Void.TYPE) {
                log.debug("方法返回类型为void，调用完成");
                return null;
            }

            if (resp.getPayload() == null || resp.getPayload().isEmpty()) {
                log.debug("响应中无返回数据");
                return null;
            }

            String returnClassName = resp.getMetadataOrDefault("return-class", returnType.getName());
            Class<?> rt = Class.forName(returnClassName);
            Object result = serializer.deserialize(resp.getPayload().toByteArray(), rt);
            log.debug("反序列化结果完成: {}", returnClassName);
            return result;
        } finally {
            // 关闭Channel
            if (channel != null && !channel.isShutdown()) {
                try {
                    log.trace("关闭gRPC channel");
                    channel.shutdownNow();
                } catch (Exception ignored) {
                    log.warn("关闭gRPC channel时发生异常", ignored);
                }
            }
        }
    }

    /**
     * 动态创建Channel，优先通过Nacos发现服务，失败则使用默认地址
     */
    private ManagedChannel createChannel() {
        String host = "localhost";
        int port = 9090;

        // 尝试通过Nacos获取服务实例
        if (serviceName != null && !serviceName.isEmpty() &&
                nacosServiceManager != null && nacosDiscoveryProperties != null) {
            try {
                log.debug("尝试通过Nacos发现服务: {}", serviceName);
                NamingService nacosNamingService = nacosServiceManager.getNamingService();
                //.getNamingService(nacosDiscoveryProperties.getNacosProperties());

                List<Instance> instances = nacosNamingService.getAllInstances(serviceName);

                if (instances == null || instances.isEmpty()) {
                    log.warn("未发现服务实例: {}", serviceName);
                } else {
                    // 随机选择一个健康的实例，实现简单的负载均衡
                    Instance inst = instances.get(random.nextInt(instances.size()));
                    host = inst.getIp();

                    Map<String, String> metadata = inst.getMetadata();

                    if (metadata != null && !metadata.isEmpty()) {
                        try {
                            String portStr = metadata.get(GRPC_PORT_KEY);
                            if (portStr != null && !portStr.isEmpty()) {
                                port = Integer.parseInt(portStr);
                                log.info("[GenericGrpcInvocationHandler] Service {} discovered from Nacos: {}:{}, instance {}/{}",
                                        serviceName, host, port, instances.indexOf(inst) + 1, instances.size());
                            } else {
                                log.warn("[GenericGrpcInvocationHandler] gRPC port not found in metadata, using default port 9090");
                                port = 9090;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("[GenericGrpcInvocationHandler] Invalid gRPC port in metadata using default port 9090", e);
                            port = 9090;
                        }
                    } else {
                        log.warn("[GenericGrpcInvocationHandler] No metadata found for instance, using default port 9090");
                    }
                }

            } catch (Exception e) {
                log.warn("[GenericGrpcInvocationHandler] Error discovering service from Nacos: {}, falling back to static address", e.getMessage(), e);
            }
        } else {
            log.debug("Nacos服务发现组件未配置或不可用");
        }

        // 如果Nacos不可用或未配置，使用默认地址
        if (host.equals("localhost") && port == 9090 && defaultAddress != null && !defaultAddress.isEmpty()) {
            String[] addressParts = parseStaticAddress();
            host = addressParts[0];
            port = Integer.parseInt(addressParts[1]);
            log.info("[GenericGrpcInvocationHandler] Using static address: {}:{}", host, port);
        }

        log.info("[GenericGrpcInvocationHandler] Creating gRPC channel to {}:{}", host, port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        // 将channel存储在ThreadLocal中
        channelThreadLocal.set(channel);
        return channel;
    }

    public void init() {
        if (this.serializer == null && beanFactory instanceof ListableBeanFactory) {
            try {
                Serializer s = ((ListableBeanFactory) beanFactory).getBean(Serializer.class);
                if (s != null) {
                    this.serializer = s;
                    log.info("[GrpcClientFactoryBean] found Serializer by type: {}", s.getClass().getName());
                }
            } catch (BeansException ignored) {
                // not found by type
                log.debug("未通过类型找到Serializer Bean");
            }
        }
        if (this.serializer == null) {
            log.info("[GrpcClientFactoryBean] no Serializer bean found in context, use DefaultJsonSerializer fallback");
            this.serializer = createDefaultJsonSerializer();
            log.info("[GrpcClientFactoryBean] created DefaultJsonSerializer: {}", this.serializer.getClass().getName());
        }

        // 2) resolve Nacos objects by type if possible (ListableBeanFactory)
        if (beanFactory instanceof ListableBeanFactory) {
            ListableBeanFactory listable = (ListableBeanFactory) beanFactory;

            // try a few known FQCNs for Nacos service manager (some versions differ)
            String[] svcManagerCandidates = new String[]{
                    "com.alibaba.cloud.nacos.NacosServiceManager",
                    "com.alibaba.nacos.client.naming.NacosNamingService", // not exactly manager, keep as candidate
                    "com.alibaba.nacos.client.config.NacosConfigService",
                    "com.alibaba.cloud.nacos.NacosServiceManager" /*duplicate safe*/
            };
            for (String fqcn : svcManagerCandidates) {
                try {
                    Class<?> cls = Class.forName(fqcn);
                    String[] names = listable.getBeanNamesForType(cls);
                    if (names != null && names.length > 0) {
                        this.nacosServiceManager = (NacosServiceManager) listable.getBean(names[0]);
                        log.info("[GrpcClientFactoryBean] resolved NacosServiceManager by class {} -> bean {}", fqcn, names[0]);
                        break;
                    }
                } catch (ClassNotFoundException ignored) {
                    // not on classpath
                    log.debug("类未找到: {}", fqcn);
                } catch (BeansException ignored) {
                    log.debug("获取Bean失败: {}", fqcn);
                }
            }

            // try common discovery properties class names
            String[] discoveryCandidates = new String[]{
                    "com.alibaba.cloud.nacos.NacosDiscoveryProperties",
                    "com.alibaba.cloud.nacos.NacosDiscoveryProperties" // duplicate safe
            };
            for (String fqcn : discoveryCandidates) {
                try {
                    Class<?> cls = Class.forName(fqcn);
                    String[] names = listable.getBeanNamesForType(cls);
                    if (names != null && names.length > 0) {
                        this.nacosDiscoveryProperties = (NacosDiscoveryProperties) listable.getBean(names[0]);
                        log.info("[GrpcClientFactoryBean] resolved NacosDiscoveryProperties by class {} -> bean {}", fqcn, names[0]);
                        break;
                    }
                } catch (ClassNotFoundException ignored) {
                    log.debug("类未找到: {}", fqcn);
                } catch (BeansException ignored) {
                    log.debug("获取Bean失败: {}", fqcn);
                }
            }
        } else {
            log.debug("[GrpcClientFactoryBean] beanFactory is not ListableBeanFactory, skip Nacos auto-resolve");
        }
    }

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
                    }
                }

                @Override
                public <R> R deserialize(byte[] data, Class<R> clazz) {
                    try {
                        return (R) readMethod.invoke(mapper, data, clazz);
                    } catch (Throwable t) {
                        throw new RuntimeException("Jackson deserialize failed", t);
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
            log.warn("Jackson未找到，使用默认序列化器", ex);
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

    private String[] parseStaticAddress() {
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
        log.debug("解析静态地址: host={}, port={}", staticHost, staticPort);
        return new String[]{staticHost, staticPort};
    }
}