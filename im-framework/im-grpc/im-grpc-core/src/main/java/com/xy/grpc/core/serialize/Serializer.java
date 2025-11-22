package com.xy.grpc.core.serialize;

public interface Serializer {
    byte[] serialize(Object obj) throws Exception;

    <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception;

    Object[] deserializeToArray(byte[] bytes, Class<?>[] paramTypes) throws Exception;

    String contentType();
}
