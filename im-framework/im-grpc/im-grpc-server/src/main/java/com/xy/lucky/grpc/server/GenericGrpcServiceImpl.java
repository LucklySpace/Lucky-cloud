package com.xy.lucky.grpc.server;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.xy.lucky.grpc.core.generic.GenericRequest;
import com.xy.lucky.grpc.core.generic.GenericResponse;
import com.xy.lucky.grpc.core.generic.GenericServiceGrpc;
import com.xy.lucky.grpc.core.serialize.Serializer;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance gRPC service implementation (generic routing)
 * 高性能 gRPC 服务实现（通用路由）
 */
@GrpcService
public class GenericGrpcServiceImpl extends GenericServiceGrpc.GenericServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GenericGrpcServiceImpl.class);
    private static final Object[] NO_ARGS = new Object[0];
    // Local ObjectMapper (fallback, for fast JsonNode operations)
    // 本地 ObjectMapper（回退方案，用于快速 JsonNode 操作）
    private static final ObjectMapper FALLBACK_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // Route registry / 路由注册表
    private final GrpcRouteRegistry registry;
    // Serializer for data transmission / 用于数据传输的序列化器
    private final Serializer serializer;
    /**
     * Local cache: url -> HandlerMeta
     * Avoid repeated reflection work and MethodHandle construction when traversing registry.find(...)
     * 本地缓存：url -> HandlerMeta
     * 避免每次都走 registry.find(...) 的反射工作与重复构建 MethodHandle
     */
    private final ConcurrentHashMap<String, HandlerMeta> metaCache = new ConcurrentHashMap<>();
    // Reflectively find and cache Serializer.deserializeToArray method
    // (signature: Object[] deserializeToArray(byte[], Class<?>[]))
    // 反射查找并缓存 Serializer.deserializeToArray 方法（签名: Object[] deserializeToArray(byte[], Class<?>[])）
    private volatile Method cachedDeserializeToArrayMethod = null;

    /**
     * Constructor for GenericGrpcServiceImpl
     * GenericGrpcServiceImpl的构造函数
     *
     * @param registry   Route registry / 路由注册表
     * @param serializer Data serializer / 数据序列化器
     */
    public GenericGrpcServiceImpl(@Lazy GrpcRouteRegistry registry,
                                  @Qualifier("JsonSerializer") Serializer serializer) {
        this.registry = registry;
        this.serializer = serializer;
    }

    @Override
    public void call(GenericRequest request, StreamObserver<GenericResponse> responseObserver) {
        final String url = request == null ? null : request.getUrl();
        if (!StringUtils.hasText(url)) {
            sendError(responseObserver, 400, "URL is required");
            // URL是必需的
            return;
        }

        // Get or initialize handler metadata (thread-safe, first load will do reflection parsing)
        // 获取或初始化 handler metadata（线程安全，首次加载会做反射解析）
        HandlerMeta meta = metaCache.computeIfAbsent(url, this::createMetaForUrl);
        if (meta == null || meta.methodHandle == null) {
            // Registry not found or meta creation failed
            // registry 找不到或创建 meta 失败
            sendError(responseObserver, 404, "No handler for " + url);
            return;
        }

        try {
            Object[] args = resolveArguments(meta, request.getPayload());
            // Directly call with MethodHandle (already bound to bean instance)
            // 直接用 MethodHandle（已 bind 到 bean 实例）调用
            Object result;
            if (args == null || args.length == 0) {
                // No arguments
                // 无参
                result = meta.methodHandle.invokeWithArguments();
            } else {
                // invokeWithArguments is faster than reflect, accepts Object...
                // invokeWithArguments 比 reflect 快，接受 Object...
                result = meta.methodHandle.invokeWithArguments(args);
            }

            // Handle response
            // 处理返回
            if (meta.isVoidReturn) {
                sendOk(responseObserver, new byte[0]);
            } else {
                byte[] payload = (result == null) ? new byte[0] : serializer.serialize(result);
                sendOk(responseObserver, payload);
            }
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
            log.error("Business exception when invoking handler for url={}", url, cause);
            // 调用URL处理器时发生业务异常
            sendError(responseObserver, 500, cause.getMessage() != null ? cause.getMessage() : "Business error");
            // 业务错误
        } catch (IllegalArgumentException iae) {
            log.warn("Bad request for url={}: {}", url, iae.getMessage());
            // URL的错误请求
            sendError(responseObserver, 400, iae.getMessage());
        } catch (Throwable t) {
            log.error("Unexpected error handling grpc request for url={}", url, t);
            // 处理URL的gRPC请求时发生未预期错误
            sendError(responseObserver, 500, "Internal server error");
            // 内部服务器错误
        }
    }

    // Create and return HandlerMeta (return null if handler not found)
    // 创建并返回 HandlerMeta（如果找不到 handler 则返回 null）
    private HandlerMeta createMetaForUrl(String url) {
        try {
            GrpcRouteHandler handler = registry.find(url);
            if (handler == null) {
                if (log.isDebugEnabled()) log.debug("No handler in registry for url={}", url);
                // 注册表中找不到URL的处理器
                return null;
            }
            Method method = handler.getMethod();
            Object bean = handler.getBean();

            // Construct a bound MethodHandle (bound to bean instance) for faster invocation
            // 构造一个 bound MethodHandle（绑定到 bean 实例），便于调用速度更快
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            // Try to set Accessible to reduce access restrictions (may fail under certain security managers)
            // 尝试设置Accessible以减少访问限制（在某些安全管理器下可能失败）
            try {
                method.setAccessible(true);
            } catch (Throwable ignored) { /* ignore */ }

            MethodHandle mh;
            try {
                mh = lookup.unreflect(method).bindTo(bean);
            } catch (IllegalAccessException e) {
                // If lookup cannot access, fall back to reflection Method call wrapped MethodHandle
                // 如果 lookup 无法访问，回退到反射 Method 调用的包装 MethodHandle
                mh = MethodHandles.lookup().unreflect(method).bindTo(bean);
            }

            Class<?>[] paramTypes = method.getParameterTypes();
            boolean isVoid = method.getReturnType() == Void.TYPE || method.getReturnType() == void.class;

            return new HandlerMeta(bean, method, mh, paramTypes, isVoid);
        } catch (Throwable ex) {
            log.error("Failed to create handler meta for url=" + url, ex);
            // 为URL创建处理器元数据失败
            return null;
        }
    }

    /**
     * Convert request payload bytes to the argument array required by the target method
     * 将请求 payload bytes 转成目标方法所需的参数数组
     */
    private Object[] resolveArguments(HandlerMeta meta, ByteString payloadBs) throws Exception {
        int paramCount = meta.paramTypes.length;
        if (paramCount == 0) return NO_ARGS;

        byte[] payload = (payloadBs == null) ? new byte[0] : payloadBs.toByteArray();
        if (payload.length == 0) {
            // If the method requires parameters but payload is empty => error
            // 如果方法需要参数但 payload 为空 => 错误
            throw new IllegalArgumentException("Payload is empty but method requires " + paramCount + " parameters");
            // 负载为空但方法需要参数
        }

        if (paramCount == 1) {
            // Single parameter path: simple, fast
            // 单参数路径：简单、快速
            Class<?> p0 = meta.paramTypes[0];
            // Fast path: if serializer can directly deserialize to target type, use it
            // 快速路径：如果 serializer 能直接反序列化成目标类型则用之
            Object arg = serializer.deserialize(payload, p0);
            return new Object[]{arg};
        }

        // Multiple parameters: prefer to call serializer's deserializeToArray(byte[], Class<?>[])
        // 多参数：优先调用 serializer 的 deserializeToArray(byte[], Class<?>[])
        try {
            // Use reflection to detect if Serializer provides deserializeToArray method
            // (avoid strong interface dependency)
            // 使用反射检测 Serializer 是否提供 deserializeToArray 方法（避免接口强依赖）
            Method deserToArray = findDeserializeToArray(serializer);
            if (deserToArray != null) {
                Object arrObj = deserToArray.invoke(serializer, payload, meta.paramTypes);
                if (arrObj instanceof Object[]) {
                    Object[] arr = (Object[]) arrObj;
                    if (arr.length != paramCount) {
                        throw new IllegalArgumentException("Parameter count mismatch: expected " + paramCount + ", got " + arr.length);
                        // 参数数量不匹配：期望得到，实际得到
                    }
                    return arr;
                }
            }
        } catch (InvocationTargetException ite) {
            // Serialization internal exception -> throw upward to return 400/500
            // 序列化内部抛出异常 -> 向上抛出以返回 400/500
            throw new IllegalArgumentException("Failed to deserialize parameters", ite.getCause());
            // 参数反序列化失败
        }

        // fallback: use Jackson ObjectMapper's JsonNode for conversion item by item
        // to reduce full array mapping overhead
        // 回退：使用 Jackson ObjectMapper 的 JsonNode 逐项转换，减少全数组映射开销
        JsonNode root = FALLBACK_MAPPER.readTree(payload);
        if (!root.isArray()) {
            throw new IllegalArgumentException("Payload is not a JSON array but method expects " + paramCount + " arguments");
            // 负载不是JSON数组但方法期望参数
        }
        if (root.size() < paramCount) {
            throw new IllegalArgumentException("wrong number of arguments: " + root.size() + " expected: " + paramCount);
            // 参数数量错误：实际得到，期望得到
        }

        Object[] args = new Object[paramCount];
        for (int i = 0; i < paramCount; i++) {
            JsonNode n = root.get(i);
            // Convert JsonNode to target type (more flexible than single readValue)
            // 将 JsonNode 转成目标类型（比单次 readValue 更灵活）
            args[i] = FALLBACK_MAPPER.treeToValue(n, meta.paramTypes[i]);
        }
        return args;
    }

    private Method findDeserializeToArray(Serializer s) {
        Method m = cachedDeserializeToArrayMethod;
        if (m != null) return m;
        synchronized (this) {
            if (cachedDeserializeToArrayMethod != null) return cachedDeserializeToArrayMethod;
            try {
                Method mm = s.getClass().getMethod("deserializeToArray", byte[].class, Class[].class);
                mm.setAccessible(true);
                cachedDeserializeToArrayMethod = mm;
            } catch (NoSuchMethodException e) {
                cachedDeserializeToArrayMethod = null; // Mark as unavailable
                // 标记为不可用
            }
            return cachedDeserializeToArrayMethod;
        }
    }

    // Send successful response
    // 发送成功响应
    private void sendOk(StreamObserver<GenericResponse> observer, byte[] payload) {
        GenericResponse resp = GenericResponse.newBuilder()
                .setCode(0)
                .setMessage("OK")
                .setPayload(ByteString.copyFrom(payload == null ? new byte[0] : payload))
                .build();
        observer.onNext(resp);
        observer.onCompleted();
    }

    // Send error response
    // 发送错误响应
    private void sendError(StreamObserver<GenericResponse> observer, int code, String message) {
        GenericResponse resp = GenericResponse.newBuilder()
                .setCode(code)
                .setMessage(message == null ? "" : message)
                .build();
        observer.onNext(resp);
        observer.onCompleted();
    }

    /**
     * Locally cached Handler metadata to avoid reflection overhead each time
     * 本地缓存的 Handler 元信息，避免每次反射开销
     */
    private static final class HandlerMeta {
        // Bean instance / Bean实例
        final Object bean;
        // Handler method / 处理方法
        final Method method;
        // Method handle bound to bean instance / 绑定到Bean实例的方法句柄
        final MethodHandle methodHandle; // bound to bean instance
        // Parameter types / 参数类型
        final Class<?>[] paramTypes;
        // Is return type void / 是否为void返回类型
        final boolean isVoidReturn;

        /**
         * Constructor for HandlerMeta
         * HandlerMeta的构造函数
         *
         * @param bean         Bean instance / Bean实例
         * @param method       Handler method / 处理方法
         * @param mh           Method handle / 方法句柄
         * @param paramTypes   Parameter types / 参数类型
         * @param isVoidReturn Is return type void / 是否为void返回类型
         */
        HandlerMeta(Object bean, Method method, MethodHandle mh, Class<?>[] paramTypes, boolean isVoidReturn) {
            this.bean = bean;
            this.method = method;
            this.methodHandle = mh;
            this.paramTypes = paramTypes != null ? paramTypes : new Class<?>[0];
            this.isVoidReturn = isVoidReturn;
        }
    }
}