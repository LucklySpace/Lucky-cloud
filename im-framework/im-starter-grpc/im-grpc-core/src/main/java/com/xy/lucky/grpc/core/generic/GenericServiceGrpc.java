package com.xy.lucky.grpc.core.generic;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 *
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.66.0)",
        comments = "Source: generic.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GenericServiceGrpc {

    public static final java.lang.String SERVICE_NAME = "generic.GenericService";
    private static final int METHODID_CALL = 0;
    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<GenericRequest,
            GenericResponse> getCallMethod;
    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    private GenericServiceGrpc() {
    }

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "Call",
            requestType = GenericRequest.class,
            responseType = GenericResponse.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<GenericRequest,
            GenericResponse> getCallMethod() {
        io.grpc.MethodDescriptor<GenericRequest, GenericResponse> getCallMethod;
        if ((getCallMethod = GenericServiceGrpc.getCallMethod) == null) {
            synchronized (GenericServiceGrpc.class) {
                if ((getCallMethod = GenericServiceGrpc.getCallMethod) == null) {
                    GenericServiceGrpc.getCallMethod = getCallMethod =
                            io.grpc.MethodDescriptor.<GenericRequest, GenericResponse>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Call"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            GenericRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            GenericResponse.getDefaultInstance()))
                                    .setSchemaDescriptor(new GenericServiceMethodDescriptorSupplier("Call"))
                                    .build();
                }
            }
        }
        return getCallMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static GenericServiceStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GenericServiceStub> factory =
                new io.grpc.stub.AbstractStub.StubFactory<GenericServiceStub>() {
                    @java.lang.Override
                    public GenericServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                        return new GenericServiceStub(channel, callOptions);
                    }
                };
        return GenericServiceStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static GenericServiceBlockingStub newBlockingStub(
            io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GenericServiceBlockingStub> factory =
                new io.grpc.stub.AbstractStub.StubFactory<GenericServiceBlockingStub>() {
                    @java.lang.Override
                    public GenericServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                        return new GenericServiceBlockingStub(channel, callOptions);
                    }
                };
        return GenericServiceBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static GenericServiceFutureStub newFutureStub(
            io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<GenericServiceFutureStub> factory =
                new io.grpc.stub.AbstractStub.StubFactory<GenericServiceFutureStub>() {
                    @java.lang.Override
                    public GenericServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                        return new GenericServiceFutureStub(channel, callOptions);
                    }
                };
        return GenericServiceFutureStub.newStub(factory, channel);
    }

    public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
        return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                .addMethod(
                        getCallMethod(),
                        io.grpc.stub.ServerCalls.asyncUnaryCall(
                                new MethodHandlers<
                                        GenericRequest,
                                        GenericResponse>(
                                        service, METHODID_CALL)))
                .build();
    }

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (GenericServiceGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                            .setSchemaDescriptor(new GenericServiceFileDescriptorSupplier())
                            .addMethod(getCallMethod())
                            .build();
                }
            }
        }
        return result;
    }

    /**
     *
     */
    public interface AsyncService {

        /**
         *
         */
        default void call(GenericRequest request,
                          io.grpc.stub.StreamObserver<GenericResponse> responseObserver) {
            io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCallMethod(), responseObserver);
        }
    }

    /**
     * Base class for the server implementation of the service GenericService.
     */
    public static abstract class GenericServiceImplBase
            implements io.grpc.BindableService, AsyncService {

        @java.lang.Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return GenericServiceGrpc.bindService(this);
        }
    }

    /**
     * A stub to allow clients to do asynchronous rpc calls to service GenericService.
     */
    public static final class GenericServiceStub
            extends io.grpc.stub.AbstractAsyncStub<GenericServiceStub> {
        private GenericServiceStub(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected GenericServiceStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GenericServiceStub(channel, callOptions);
        }

        /**
         *
         */
        public void call(GenericRequest request,
                         io.grpc.stub.StreamObserver<GenericResponse> responseObserver) {
            io.grpc.stub.ClientCalls.asyncUnaryCall(
                    getChannel().newCall(getCallMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     * A stub to allow clients to do synchronous rpc calls to service GenericService.
     */
    public static final class GenericServiceBlockingStub
            extends io.grpc.stub.AbstractBlockingStub<GenericServiceBlockingStub> {
        private GenericServiceBlockingStub(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected GenericServiceBlockingStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GenericServiceBlockingStub(channel, callOptions);
        }

        /**
         *
         */
        public GenericResponse call(GenericRequest request) {
            return io.grpc.stub.ClientCalls.blockingUnaryCall(
                    getChannel(), getCallMethod(), getCallOptions(), request);
        }
    }

    /**
     * A stub to allow clients to do ListenableFuture-style rpc calls to service GenericService.
     */
    public static final class GenericServiceFutureStub
            extends io.grpc.stub.AbstractFutureStub<GenericServiceFutureStub> {
        private GenericServiceFutureStub(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected GenericServiceFutureStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new GenericServiceFutureStub(channel, callOptions);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<GenericResponse> call(
                GenericRequest request) {
            return io.grpc.stub.ClientCalls.futureUnaryCall(
                    getChannel().newCall(getCallMethod(), getCallOptions()), request);
        }
    }

    private static final class MethodHandlers<Req, Resp> implements
            io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
        private final AsyncService serviceImpl;
        private final int methodId;

        MethodHandlers(AsyncService serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_CALL:
                    serviceImpl.call((GenericRequest) request,
                            (io.grpc.stub.StreamObserver<GenericResponse>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(
                io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

    private static abstract class GenericServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
        GenericServiceBaseDescriptorSupplier() {
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return GenericProto.getDescriptor();
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("GenericService");
        }
    }

    private static final class GenericServiceFileDescriptorSupplier
            extends GenericServiceBaseDescriptorSupplier {
        GenericServiceFileDescriptorSupplier() {
        }
    }

    private static final class GenericServiceMethodDescriptorSupplier
            extends GenericServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
        private final java.lang.String methodName;

        GenericServiceMethodDescriptorSupplier(java.lang.String methodName) {
            this.methodName = methodName;
        }

        @java.lang.Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }
}
