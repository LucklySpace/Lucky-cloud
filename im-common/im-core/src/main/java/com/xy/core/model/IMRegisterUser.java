package com.xy.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
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
     * 当前设备的唯一标识（可为 UUID、设备序列号等）
     */
    private String deviceId;

    /**
     * 设备类型（如：android、ios、windows、mac、web 等）
     */
    private String deviceType;

    /**
     * 客户端应用版本号（如：1.0.3）
     */
    private String clientVersion;

    /**
     * 客户端平台（可选字段，标识运行平台）
     * 示例：Android、iOS、Windows、Linux、Web
     */
    private String platform;
}