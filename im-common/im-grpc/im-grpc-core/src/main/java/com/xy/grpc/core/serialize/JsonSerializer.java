package com.xy.grpc.core.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component("JsonSerializer")
public class JsonSerializer implements Serializer {
    private static final ObjectMapper M = new ObjectMapper();

    @Override
    public byte[] serialize(Object obj) throws Exception {
        return M.writeValueAsBytes(obj);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        return M.readValue(bytes, clazz);
    }

    @Override
    public String contentType() {
        return "application/json";
    }
}
