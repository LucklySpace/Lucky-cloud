package com.xy.crypto.core.domain;


/**
 * 加密模式枚举
 */
public enum CryptoMode {
    // 使用配置文件中的默认加密设置
    GLOBAL,
    // 不加密
    NONE,
    // AES加密
    AES,
    // RSA加密
    RSA
}
