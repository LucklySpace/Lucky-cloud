package com.xy.grpc.server.server;

import com.google.protobuf.ByteString;
import com.xy.grpc.core.generic.GenericRequest;
import com.xy.grpc.core.generic.GenericResponse;
import com.xy.grpc.core.generic.GenericServiceGrpc;
import com.xy.grpc.core.serialize.Serializer;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;

@GrpcService
public class GenericGrpcServiceImpl extends GenericServiceGrpc.GenericServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(GenericGrpcServiceImpl.class);

    private final GrpcRouteRegistry registry;
    private final Serializer serializer;

    public GenericGrpcServiceImpl(@Lazy GrpcRouteRegistry registry, @Qualifier("JsonSerializer") Serializer serializer) {
        this.registry = registry;
        this.serializer = serializer;
    }

    @Override
    public void call(GenericRequest request, StreamObserver<GenericResponse> responseObserver) {
        String url = request.getUrl();
        if (!StringUtils.hasText(url)) {
            log.warn("Invalid request: URL is empty");
            sendErrorResponse(responseObserver, 400, "URL is required");
            return;
        }

        log.debug("开始处理gRPC请求: {}", url);

        try {
            GrpcRouteHandler handler = registry.find(url);
            if (handler == null) {
                log.warn("未找到处理程序: {}", url);
                sendErrorResponse(responseObserver, 404, "No handler for " + url);
                return;
            }

            Class<?>[] paramTypes = handler.getParamTypes();
            Object[] args = null;
            if (paramTypes.length > 0) {
                byte[] payloadBytes = request.getPayload().toByteArray();
                if (payloadBytes.length == 0) {
                    throw new IllegalArgumentException("Payload is empty but method requires parameters");
                }
                if (paramTypes.length == 1) {
                    // 单参数：反序列化为单个对象
                    args = new Object[1];
                    args[0] = serializer.deserialize(payloadBytes, paramTypes[0]);
                    log.debug("反序列化单参数完成: {}", paramTypes[0].getSimpleName());
                } else {
                    // 多参数：反序列化为数组（假设 payload 是序列化的数组）
                    args = serializer.deserializeToArray(payloadBytes, paramTypes);
                    if (args.length != paramTypes.length) {
                        throw new IllegalArgumentException("Parameter count mismatch: expected " + paramTypes.length + ", got " + args.length);
                    }
                    log.debug("反序列化多参数完成: {} 个参数", paramTypes.length);
                }
            } else {
                log.debug("方法无参数");
            }

            Object result = handler.getMethod().invoke(handler.getBean(), args);
            log.debug("方法调用成功: {}", handler.getMethod().getName());

            byte[] payload = (result != null && !handler.getReturnType().equals(Void.class))
                    ? serializer.serialize(result)
                    : new byte[0];
            GenericResponse response = GenericResponse.newBuilder()
                    .setCode(0)
                    .setMessage("OK")
                    .setPayload(ByteString.copyFrom(payload))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.debug("请求处理完成: {}", url);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("调用目标方法时发生业务异常: {} - {}", url, cause.getMessage(), cause);
            sendErrorResponse(responseObserver, 500, cause.getMessage() != null ? cause.getMessage() : "Business error");
        } catch (IllegalArgumentException | IllegalAccessException e) {
            log.error("参数或访问异常: {} - {}", url, e.getMessage(), e);
            sendErrorResponse(responseObserver, 400, e.getMessage() != null ? e.getMessage() : "Invalid request");
        } catch (Exception e) {
            log.error("处理gRPC请求时发生系统异常: {} - {}", url, e.getMessage(), e);
            sendErrorResponse(responseObserver, 500, e.getMessage() != null ? e.getMessage() : "Internal server error");
        }
    }

    private void sendErrorResponse(StreamObserver<GenericResponse> responseObserver, int code, String message) {
        GenericResponse errorResponse = GenericResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .build();
        responseObserver.onNext(errorResponse);
        responseObserver.onCompleted();
    }
}