package com.xy.lucky.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * IM 用户注册/登录请求实体，用于客户端连接 Netty 时的身份认证。
 * 支持多端登录，每个设备需唯一标识。
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class IMRegisterUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识（账号 ID）
     */
    private String userId;

    /**
     * 登录授权令牌（后端校验）
     */
    private String token;

    /**
     * 分配的消息路由 brokerId（可用于消息路由/分片）
     */
    private String brokerId;

    /**
     * 多平台设备信息
     */
    private Map<String, Driver> drivers;

    /**
     * 设备信息  设备类型（如：android、ios、windows、mac、web 等）
     * @param deviceId 当前设备的唯一标识（可为 UUID、设备序列号等）
     * @param deviceType 设备类型（如：android、ios、windows、mac、web 等）
     * @param clientVersion 客户端应用版本号（如：1.0.3）
     */
    public record Driver(String deviceId, String deviceType) {
    }
}