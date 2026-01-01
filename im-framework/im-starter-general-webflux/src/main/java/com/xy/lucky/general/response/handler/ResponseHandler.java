package com.xy.lucky.general.response.handler;

import com.xy.lucky.general.response.domain.Result;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 自定义响应处理器，用于统一包装控制器返回值为 Result 对象。
 *
 * - 修复 Jackson: "Null key for a Map not allowed in JSON" 问题：
 *   在包装返回值之前，对 Map 的 key 做 sanitize（将 null key 转为 "null" 或丢弃）。
 * - 支持 Mono/Flux/同步返回值的包装。
 * - 排除 Void、Result 和 ResponseEntity 类型。
 */
public class ResponseHandler extends ResponseBodyResultHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);

    public ResponseHandler(List<HttpMessageWriter<?>> writers,
                           RequestedContentTypeResolver resolver,
                           ReactiveAdapterRegistry registry) {
        super(writers, resolver, registry);
    }

    @Override
    public boolean supports(HandlerResult result) {
        Class<?> returnType = result.getReturnType().resolve();
        if (returnType == null || returnType == Void.TYPE || returnType == Void.class) {
            return false;
        }
        if (Result.class.isAssignableFrom(returnType) || org.springframework.http.ResponseEntity.class.isAssignableFrom(returnType)) {
            return false;
        }
        return true;
    }

    @Override
    public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
        // 强制设置响应 Content-Type 为 JSON，避免文本转换导致的 ClassCastException
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Object returnValue = result.getReturnValue();

        // 创建包装后的 Publisher（同时在包装前对要序列化的 data 进行 sanitize）
        Publisher<?> bodyPublisher = createWrappedPublisher(returnValue);

        // 构建新的 MethodParameter 以匹配包装后的类型（修复类型不匹配）
        MethodParameter wrappedType = buildWrappedMethodParameter(result.getReturnTypeSource(), returnValue);

        return writeBody(bodyPublisher, wrappedType, exchange)
                .doOnError(e -> log.error("响应处理异常: 方法={}, 值类型={}, 异常={}",
                        result.getHandler(),
                        (returnValue == null ? "null" : returnValue.getClass().getName()),
                        e.getMessage(), e));
    }

    /**
     * 创建包装后的 Publisher（Mono<Result> 或类似）。
     * 在包装成 Result 前对对象进行 sanitize（避免 Map 包含 null key 导致 Jackson 序列化异常）。
     */
    private Publisher<?> createWrappedPublisher(Object returnValue) {
        if (returnValue instanceof Mono<?> mono) {
            return mono.map(value -> {
                        if (value instanceof Result) {
                            // 对内部 Result 的 data 字段也进行 sanitize
                            sanitizeResultIfNeeded((Result<?>) value);
                            return value;
                        } else {
                            Object sanitized = sanitizeForJson(value);
                            return Result.success(sanitized);
                        }
                    })
                    .defaultIfEmpty(Result.success()); // 空 Mono -> 空 Result
        } else if (returnValue instanceof Flux<?> flux) {
            // 将 Flux 收集为 List 并包装为 Result<List<T>>
            return flux.collectList().map(list -> {
                Object sanitized = sanitizeForJson(list);
                return Result.success(sanitized);
            });
        } else {
            // 同步返回值或 null
            if (returnValue == null) {
                return Mono.just(Result.success());
            }
            if (returnValue instanceof Result) {
                sanitizeResultIfNeeded((Result<?>) returnValue);
                return Mono.just(returnValue);
            } else {
                Object sanitized = sanitizeForJson(returnValue);
                return Mono.just(Result.success(sanitized));
            }
        }
    }

    /**
     * 如果传入的是 Result 实例，尝试对其 data 字段进行 sanitize（保护外部直接返回的 Result）。
     */
    @SuppressWarnings("unchecked")
    private <T> void sanitizeResultIfNeeded(Result<T> result) {
        if (result == null) return;
        try {
            if (result.getData() != null) {
                try {
                    // 优先使用公开方法设置
                    result.setData((T) sanitizeForJson(result.getData()));
                } catch (NoSuchMethodError | Exception ignored) {
                    // 如果不能直接设置，则仅记录日志（不破坏现有对象）
                    log.debug("Result#setData not available, sanitized value not set directly.");
                }
            }
        } catch (Throwable t) {
            log.warn("sanitizeResultIfNeeded failed: {}", t.getMessage());
        }
    }

    /**
     * 构建新的 MethodParameter，反映包装后的类型 (e.g., Mono<Result<T>> 或 Result<T>)
     */
    private MethodParameter buildWrappedMethodParameter(MethodParameter original, Object returnValue) {
        ResolvableType originalResolvable = ResolvableType.forMethodParameter(original);

        // 尝试读取容器类型（Mono<T> / Flux<T> / 直接 T）
        ResolvableType containerResolvable = originalResolvable;
        Class<?> containerClass = containerResolvable.resolve(); // 例如 Mono.class / Flux.class / POJO class

        // 获取原始内层类型 (对于 Mono<T>/Flux<T>，innerType = T；否则 innerType = 原始类型)
        ResolvableType innerType = containerResolvable.hasGenerics() ? containerResolvable.getGeneric(0) : containerResolvable;

        // 构造 Result<Inner>
        ParameterizedType resultType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                Type t = innerType.getType();
                // 如果 innerType 无法解析，退回 Object.class
                return new Type[]{t == null ? Object.class : t};
            }

            @Override
            public Type getRawType() {
                return Result.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        final Type wrappedGenericType;
        final Class<?> wrappedContainerClass;

        if (containerClass != null && (Mono.class.isAssignableFrom(containerClass) || returnValue == null || !(returnValue instanceof Publisher))) {
            // Mono 或 同步：Mono<Result<T>>
            wrappedGenericType = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{resultType};
                }

                @Override
                public Type getRawType() {
                    return Mono.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };
            wrappedContainerClass = Mono.class;
        } else if (containerClass != null && Flux.class.isAssignableFrom(containerClass)) {
            // Flux -> 我们在 createWrappedPublisher 中把 Flux.collectList() 映射为 Mono<Result<List<T>>>
            // 因此这里声明为 Mono<Result<List<T>>>
            ParameterizedType listType = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    Type t = innerType.getType();
                    return new Type[]{t == null ? Object.class : t};
                }

                @Override
                public Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };

            ParameterizedType resultListType = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{listType};
                }

                @Override
                public Type getRawType() {
                    return Result.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };

            wrappedGenericType = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{resultListType};
                }

                @Override
                public Type getRawType() {
                    return Mono.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };
            wrappedContainerClass = Mono.class;
        } else {
            // 同步非 Publisher：Result<T>
            wrappedGenericType = resultType;
            wrappedContainerClass = Result.class;
        }

        // 创建新的 MethodParameter，使用自定义 generic type
        return new MethodParameter(original.getMethod(), -1) {
            @Override
            public Type getGenericParameterType() {
                return wrappedGenericType;
            }

            @Override
            public Class<?> getParameterType() {
                return wrappedContainerClass;
            }
        };
    }

    // --------------------------
    // JSON-safe sanitize helpers
    // --------------------------

    /**
     * 对对象进行 JSON-safe sanitize：
     * - Map: 将 key 为 null 的条目 key 转成字符串 "null"（或丢弃，视策略），并递归处理 value
     * - Collection/数组: 递归处理元素
     * - 其它原子类型返回原样
     */
    @SuppressWarnings("unchecked")
    private Object sanitizeForJson(Object obj) {
        if (obj == null) return null;

        try {
            if (obj instanceof Map<?, ?> map) {
                boolean hadNullKey = false;
                Map<String, Object> sanitized = new LinkedHashMap<>(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object rawKey = entry.getKey();
                    Object rawVal = entry.getValue();

                    if (rawKey == null) {
                        hadNullKey = true;
                        // 将 null key 转为字符串 "null"，保留数据
                        String keyStr = "null";
                        sanitized.put(keyStr, sanitizeForJson(rawVal));
                    } else {
                        // 将非字符串 key 转为字符串（JSON 对象的键必须是字符串）
                        String keyStr = String.valueOf(rawKey);
                        sanitized.put(keyStr, sanitizeForJson(rawVal));
                    }
                }
                if (hadNullKey) {
                    log.warn("ResponseHandler sanitized a Map with null key. Converted null key to string \"null\". " +
                            "If you'd prefer to drop null-key entries, change sanitize logic.");
                }
                return sanitized;
            } else if (obj instanceof Collection<?> coll) {
                List<Object> sanitizedList = new ArrayList<>(coll.size());
                for (Object item : coll) {
                    sanitizedList.add(sanitizeForJson(item));
                }
                return sanitizedList;
            } else if (obj.getClass().isArray()) {
                int len = java.lang.reflect.Array.getLength(obj);
                List<Object> sanitizedList = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    Object item = java.lang.reflect.Array.get(obj, i);
                    sanitizedList.add(sanitizeForJson(item));
                }
                return sanitizedList;
            } else {
                // primitive / POJO: leave it to Jackson. We do not deep-copy POJO fields here to avoid heavy reflection cost.
                return obj;
            }
        } catch (Throwable t) {
            log.warn("sanitizeForJson failed for object of type {}: {}", obj.getClass().getName(), t.getMessage());
            // 保守返回原对象（以避免进一步破坏）
            return obj;
        }
    }
}
