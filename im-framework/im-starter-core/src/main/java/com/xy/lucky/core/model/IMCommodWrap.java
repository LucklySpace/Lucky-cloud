package com.xy.lucky.core.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * WebSocket / IM connect message container
 * <p>
 * 泛型 T 用于承载任意业务 payload（配合 Jackson 的多态反序列化）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IMCommodWrap<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 连接/消息类型（使用枚举提高可读性）
     */
    private Integer code;

    /**
     * 用户 token（可为空）
     */
    private String token;

    /**
     * 业务数据（泛型，常用 JSON 多态来区分实际类型）
     */
    private T data;

    /**
     * 可选：附加元数据（路由/版本/平台等）
     */
    private Map<String, String> metadata;

    /**
     * 时间戳（毫秒）
     */
    @Builder.Default
    private Long timestamp = Instant.now().toEpochMilli();


}
