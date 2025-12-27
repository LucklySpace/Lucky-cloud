package com.xy.lucky.crypto.core.crypto.core;

import com.xy.lucky.crypto.core.crypto.domain.CryptoMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 策略执行器，统一管理和调用各加解密算法实现类。
 * 通过枚举映射快速定位具体算法实现，避免条件分支。
 */
@Component
public class CryptoExecutor {

    /**
     * 加解密策略映射表，键为算法模式，值为对应的算法实现。
     */
    private final Map<CryptoMode, CryptoAlgorithm> algorithmMap = new EnumMap<>(CryptoMode.class);

    /**
     * 注入所有策略实现类并完成注册。
     */
    @Autowired
    public CryptoExecutor(List<CryptoAlgorithm> algorithms) {
        for (CryptoAlgorithm alg : algorithms) {
            CryptoMode mode = alg.mode();
            // 后注册的同类型算法将覆盖前者，保证最终唯一性。
            algorithmMap.put(mode, alg);
        }
    }


    /**
     * 加密方法，根据模式调用具体算法。
     * 当模式为 NONE 时直接返回原始数据。
     */
    public String encrypt(String data, CryptoMode mode) throws Exception {
        if (mode == CryptoMode.NONE) {
            return data;
        }
        return getAlgorithm(mode).encrypt(data);
    }

    /**
     * 解密方法，根据模式调用具体算法。
     * 当模式为 NONE 时直接返回原始数据。
     */
    public String decrypt(String data, CryptoMode mode) throws Exception {
        if (mode == CryptoMode.NONE) {
            return data;
        }
        return getAlgorithm(mode).decrypt(data);
    }

    /**
     * 获取策略实现类。
     * 仅支持声明在 {@link CryptoMode} 中的具体算法模式（不包含 GLOBAL/NONE）。
     */
    private CryptoAlgorithm getAlgorithm(CryptoMode mode) {
        return Optional.ofNullable(algorithmMap.get(mode))
                .orElseThrow(() -> new IllegalArgumentException("不支持的加解密模式: " + mode));
    }
}
