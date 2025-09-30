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
        log.debug("开始处理gRPC请求: {}", url);

        try {
            GrpcRouteHandler h = registry.find(url);
            if (h == null) {
                log.warn("未找到处理程序: {}", url);
                GenericResponse r = GenericResponse.newBuilder()
                        .setCode(404)
                        .setMessage("No handler for " + url)
                        .build();
                responseObserver.onNext(r);
                responseObserver.onCompleted();
                return;
            }

            Object arg = null;
            if (!h.getParamType().equals(Void.class)) {
                arg = serializer.deserialize(request.getPayload().toByteArray(), h.getParamType());
                log.debug("反序列化参数完成: {}", h.getParamType().getSimpleName());
            }

            Object result = h.getMethod().invoke(h.getBean(), arg);
            log.debug("方法调用成功: {}", h.getMethod().getName());

            byte[] payload = result != null ? serializer.serialize(result) : new byte[0];
            GenericResponse resp = GenericResponse.newBuilder()
                    .setCode(0)
                    .setMessage("OK")
                    .setPayload(ByteString.copyFrom(payload))
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();

            log.debug("请求处理完成: {}", url);
        } catch (InvocationTargetException ite) {
            Throwable t = ite.getCause() != null ? ite.getCause() : ite;
            log.error("调用目标方法时发生异常: " + url, t);
            GenericResponse r = GenericResponse.newBuilder()
                    .setCode(500)
                    .setMessage(t.getMessage() != null ? t.getMessage() : "Internal server error")
                    .build();
            responseObserver.onNext(r);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("处理gRPC请求时发生异常: " + url, ex);
            GenericResponse r = GenericResponse.newBuilder()
                    .setCode(500)
                    .setMessage(ex.getMessage() != null ? ex.getMessage() : "Internal server error")
                    .build();
            responseObserver.onNext(r);
            responseObserver.onCompleted();
        }
    }
}