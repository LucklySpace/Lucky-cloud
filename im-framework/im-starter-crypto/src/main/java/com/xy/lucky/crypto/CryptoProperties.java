package com.xy.lucky.crypto;

import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import com.xy.lucky.crypto.core.sign.domain.SignatureMode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 加解密配置属性类，支持从 application.yml 中读取配置
 *
 * 配置示例:
 * <pre>
 * security:
 *   enabled: true
 *   crypto:
 *     enabled: true
 *     default-encrypt: AES
 *     default-decrypt: AES
 *     aes-key: your-16-24-32-byte-key
 *     aes-mode: GCM
 *     sm4-key: your-16-byte-sm4-key
 *     rsa-public-key: base64-encoded-public-key
 *     rsa-private-key: base64-encoded-private-key
 *     exclude-paths:
 *       - /api/public/**
 *       - /health
 *   sign:
 *     enabled: true
 *     mode: HMAC_SHA256
 *     secret: your-secret-key
 *     timestamp-tolerance: 300000
 *     nonce-enabled: true
 * </pre>
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "security")
public class CryptoProperties {

    /**
     * 是否启用整体安全模块
     */
    private boolean enabled = true;

    /**
     * 加解密配置
     */
    private Crypto crypto = new Crypto();

    /**
     * 签名配置
     */
    private Sign sign = new Sign();

    /**
     * AES 加密模式枚举
     */
    public enum AesMode {
        /**
         * ECB 模式（不推荐，仅用于兼容旧系统）
         */
        ECB,
        /**
         * CBC 模式（需要 IV）
         */
        CBC,
        /**
         * GCM 模式（推荐，提供认证加密）
         */
        GCM
    }

    @Data
    public static class Crypto {
        /**
         * 是否启用加解密功能
         */
        private boolean enabled = true;

        /**
         * 默认加密模式（用于响应）
         */
        private CryptoMode defaultEncrypt = CryptoMode.AES;

        /**
         * 默认解密模式（用于请求）
         */
        private CryptoMode defaultDecrypt = CryptoMode.AES;

        /**
         * AES 密钥（支持 16/24/32 位）
         */
        private String aesKey = "defaultaeskey123";

        /**
         * AES 加密模式: GCM（推荐）/ CBC / ECB
         */
        private AesMode aesMode = AesMode.GCM;

        /**
         * AES CBC/GCM 模式的初始化向量（可选，不配置则自动生成）
         */
        private String aesIv;

        /**
         * RSA 公钥（Base64 编码）
         */
        private String rsaPublicKey;

        /**
         * RSA 私钥（Base64 编码）
         */
        private String rsaPrivateKey;

        /**
         * RSA 密钥长度（默认2048）
         */
        private int rsaKeySize = 2048;

        /**
         * 排除的路径（支持 Ant 风格通配符）
         */
        private List<String> excludePaths = new ArrayList<>();

        /**
         * 是否在响应中包含加密算法标识
         */
        private boolean includeAlgorithmHeader = false;

        /**
         * 加密算法标识 Header 名称
         */
        private String algorithmHeaderName = "X-Crypto-Algorithm";

        /**
         * 是否启用调试模式（打印加解密日志）
         */
        private boolean debug = false;
    }

    @Data
    public static class Sign {
        /**
         * 是否启用签名功能
         */
        private boolean enabled = true;

        /**
         * 默认签名算法
         */
        private SignatureMode mode = SignatureMode.HMAC_SHA256;

        /**
         * 签名密钥
         */
        private String secret;

        /**
         * 时间戳容差（毫秒），默认5分钟
         */
        private long timestampTolerance = 300000L;

        /**
         * 是否启用 nonce 防重放
         */
        private boolean nonceEnabled = true;

        /**
         * nonce 缓存过期时间（秒）
         */
        private long nonceExpireSeconds = 300L;

        /**
         * 签名字段名
         */
        private String signFieldName = "sign";

        /**
         * 时间戳字段名
         */
        private String timestampFieldName = "timestamp";

        /**
         * nonce 字段名
         */
        private String nonceFieldName = "nonce";

        /**
         * 排除的路径
         */
        private List<String> excludePaths = new ArrayList<>();
    }
}
