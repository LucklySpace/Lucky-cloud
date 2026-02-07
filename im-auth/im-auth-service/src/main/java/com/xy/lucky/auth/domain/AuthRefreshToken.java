package com.xy.lucky.auth.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 刷新令牌元数据
 * <p>
 * 包含令牌版本控制、令牌族追踪、设备绑定等安全特性，
 * 支持令牌轮换和重用攻击检测。
 *
 * @author dense
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "刷新令牌元数据")
public class AuthRefreshToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    private String userId;

    /**
     * 设备唯一标识
     */
    @Schema(description = "设备唯一标识")
    private String deviceId;

    /**
     * 签发时客户端 IP
     */
    @Schema(description = "签发时客户端 IP")
    private String clientIp;

    /**
     * 签发时间戳（毫秒）
     */
    @Schema(description = "签发时间戳")
    private long issuedAt;

    /**
     * 令牌版本号
     * <p>
     * 用于实现踢人功能：当用户修改密码、主动登出所有设备、
     * 或管理员强制下线时，递增此版本号即可使所有旧令牌失效。
     */
    @Schema(description = "令牌版本号，用于踢人和会话失效控制")
    private long tokenVersion;

    /**
     * 令牌族 ID (Token Family ID)
     * <p>
     * 同一登录会话产生的所有刷新令牌共享同一个族ID。
     * 用于检测令牌重用攻击：当检测到已撤销的令牌被再次使用时，
     * 可以立即撤销整个令牌族。
     */
    @Schema(description = "令牌族ID，用于检测重用攻击")
    private String tokenFamilyId;

    /**
     * 令牌在族中的序号
     * <p>
     * 每次刷新时递增，用于追踪令牌轮换链条。
     */
    @Schema(description = "令牌在族中的序号")
    private int sequenceNumber;

    /**
     * 是否已被使用（用于单次使用检测）
     */
    @Schema(description = "令牌是否已被使用")
    private boolean used;

    /**
     * 绝对过期时间戳（毫秒）
     * <p>
     * 即使令牌不断刷新，也不能超过此时间。
     * 强制用户定期重新认证。
     */
    @Schema(description = "绝对过期时间戳")
    private long absoluteExpiresAt;
}
