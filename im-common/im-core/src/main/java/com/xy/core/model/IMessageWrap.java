package com.xy.core.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
public class IMessageWrap<T> implements Serializable {

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
     * 可选：目标用户 ID（用于定向发送消息）
     */
    private List<String> ids;

    /**
     * 可选：附加元数据（路由/版本/平台等）
     */
    private Map<String, String> metadata;

    /**
     * 可选：信息
     */
    private String message;

    /**
     * 可选客户端信息 IP
     */
    private String clientIp;

    /**
     * 可选客户端信息 UA
     */
    private String userAgent;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 请求 ID（用于链路追踪，默认可由客户端/服务器生成）
     */
    @Builder.Default
    private String requestId = UUID.randomUUID().toString();

    /**
     * 时间戳（毫秒）
     */
    @Builder.Default
    private Long timestamp = Instant.now().toEpochMilli();

    @Override
    public String toString() {
        // 避免打印 data 的全部内容（可能很大），可按需调整
        return "IMessageWrap{" +
                "code=" + code +
                ", token='" + token + '\'' +
                ", message='" + message + '\'' +
                ", dataType=" + (data != null ? data.getClass().getSimpleName() : "null") +
                ", requestId=" + requestId +
                ", timestamp=" + timestamp +
                ", clientIp='" + clientIp + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceType='" + deviceType + '\'' +
                '}';
    }

}
