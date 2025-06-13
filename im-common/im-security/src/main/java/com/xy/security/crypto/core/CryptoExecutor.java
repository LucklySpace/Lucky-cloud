package com.xy.security.crypto.core;

import com.xy.security.crypto.domain.CryptoMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 策略执行器，统一管理和调用各加解密算法实现类。
 */
@Component
public class CryptoExecutor {

    /**
     * 加解密策略映射表
     */
    private final Map<CryptoMode, CryptoAlgorithm> algorithmMap = new EnumMap<>(CryptoMode.class);

    /**
     * 注入所有策略实现类
     */
    @Autowired
    public CryptoExecutor(List<CryptoAlgorithm> algorithms) {
        for (CryptoAlgorithm alg : algorithms) {
            algorithmMap.put(alg.mode(), alg);
        }
    }


    /**
     * 加密方法，根据模式调用具体算法
     */
    public String encrypt(String data, CryptoMode mode) throws Exception {
        return getAlgorithm(mode).encrypt(data);
    }

    /**
     * 解密方法，根据模式调用具体算法
     */
    public String decrypt(String data, CryptoMode mode) throws Exception {
        return getAlgorithm(mode).decrypt(data);
    }

    /**
     * 获取策略实现类
     */
    private CryptoAlgorithm getAlgorithm(CryptoMode mode) {
        if (mode == CryptoMode.NONE) return null;
        return Optional.ofNullable(algorithmMap.get(mode))
                .orElseThrow(() -> new IllegalArgumentException("不支持的加解密模式: " + mode));
    }
}
