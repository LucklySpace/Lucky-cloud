package com.xy.lucky.crypto.core.core;

import com.xy.lucky.crypto.core.domain.CryptoMode;

/**
 * 加解密策略接口，各种算法需实现此接口。
 */
public interface CryptoAlgorithm {

    /**
     * 加密方法
     *
     * @param plainText 原始明文
     * @return 加密后的字符串
     */
    String encrypt(String plainText) throws Exception;

    /**
     * 解密方法
     *
     * @param cipherText 密文
     * @return 解密后的明文
     */
    String decrypt(String cipherText) throws Exception;

    /**
     * 当前算法支持的模式类型（如 AES、RSA）
     */
    CryptoMode mode();
}
