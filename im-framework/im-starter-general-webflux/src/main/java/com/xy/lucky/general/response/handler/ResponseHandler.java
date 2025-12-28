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
import java.util.List;

/**
 * 自定义响应处理器，用于统一包装控制器返回值为 Result 对象。
 * - 修复类型不匹配问题：动态构建新的 MethodParameter 以匹配包装后的类型。
 * - 强制设置 Content-Type 为 application/json 避免 CharSequence 转换错误。
 * - 支持 Mono/Flux/同步返回值的包装。
 * - 排除 Void、Result 和 ResponseEntity 类型。
 * - 优化：延迟类型解析，仅在需要时构建 ParameterizedType；添加日志。
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
        Publisher<?> bodyPublisher = createWrappedPublisher(returnValue);

        // 构建新的 MethodParameter 以匹配包装后的类型（修复类型不匹配）
        MethodParameter wrappedType = buildWrappedMethodParameter(result.getReturnTypeSource(), returnValue);

        return writeBody(bodyPublisher, wrappedType, exchange)
                .doOnError(e -> log.error("响应处理异常: 方法={}, 值={}, 异常={}",
                        result.getHandler(), returnValue, e.getMessage(), e));
    }

    /**
     * 创建包装后的 Publisher（Mono<Result> 或类似）。
     */
    private Publisher<?> createWrappedPublisher(Object returnValue) {
        if (returnValue instanceof Mono<?> mono) {
            return mono.map(value -> value instanceof Result ? value : Result.success(value))
                    .defaultIfEmpty(Result.success());
        } else if (returnValue instanceof Flux<?> flux) {
            return flux.collectList().map(Result::success);
        } else {
            return Mono.just(returnValue == null ? Result.success() :
                    (returnValue instanceof Result ? returnValue : Result.success(returnValue)));
        }
    }

    /**
     * 构建新的 MethodParameter，反映包装后的类型 (e.g., Mono<Result<T>>)。
     */
    private MethodParameter buildWrappedMethodParameter(MethodParameter original, Object returnValue) {
        ResolvableType originalResolvable = ResolvableType.forMethodParameter(original);
        Class<?> containerClass = originalResolvable.resolve(); // e.g., Mono or Flux or raw class

        // 获取原始内层类型 (e.g., List<EmojiPackVo>)
        ResolvableType innerType = originalResolvable.getGeneric(0); // 对于 Mono<T> 或 Flux<T>，获取 T

        // 构建 Result<T> 的 ParameterizedType
        ParameterizedType resultType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{innerType.getType()};
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

        if (Mono.class.isAssignableFrom(containerClass) || returnValue == null || !(returnValue instanceof Publisher)) {
            // 对于 Mono 或 同步：Mono<Result<T>>
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
        } else if (Flux.class.isAssignableFrom(containerClass)) {
            // 对于 Flux：Mono<Result<List<T>>> (因为 collectList())
            ParameterizedType listType = new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[]{innerType.getType()};
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
}


