package com.xy.lucky.crypto.core.sign.core;

import com.xy.lucky.crypto.core.sign.domain.SignatureMode;

import java.util.Map;

/**
 * 签名算法接口
 */
public interface SignatureAlgorithm {

    /**
     * 获取算法类型
     */
    SignatureMode mode();

    /**
     * 计算签名
     *
     * @param data 待签名数据
     * @return 签名字符串
     */
    String sign(Map<String, String> data) throws Exception;

    /**
     * 验证签名
     *
     * @param data      原始数据
     * @param signature 签名
     * @return 是否验证通过
     */
    boolean verify(Map<String, String> data, String signature) throws Exception;

    /**
     * 计算签名（带排除字段）
     */
    default String sign(Map<String, String> data, String[] excludeFields) throws Exception {
        return sign(data);
    }

    /**
     * 验证签名（带排除字段）
     */
    default boolean verify(Map<String, String> data, String signature, String[] excludeFields) throws Exception {
        return verify(data, signature);
    }
}
