package com.xy.lucky.crypto.core.sign.domain;

/**
 * 签名算法枚举
 */
public enum SignatureMode {
    /**
     * HMAC-SHA256 签名算法（推荐）
     */
    HMAC_SHA256,

    /**
     * HMAC-SHA512 签名算法
     */
    HMAC_SHA512,

    /**
     * RSA-SHA256 签名算法
     */
    RSA_SHA256,

    /**
     * SM3 国密哈希签名
     */
    SM3
}
