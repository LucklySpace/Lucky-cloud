package com.xy.lucky.crypto.core.crypto.domain;

/**
 * 加密模式枚举
 */
public enum CryptoMode {
    /**
     * 使用配置文件中的默认加密设置
     */
    GLOBAL,

    /**
     * 不加密
     */
    NONE,

    /**
     * AES 对称加密（推荐）
     */
    AES,

    /**
     * RSA 非对称加密
     */
    RSA,

    /**
     * AES + RSA 混合加密（RSA加密AES密钥，AES加密数据）
     */
    HYBRID
}
