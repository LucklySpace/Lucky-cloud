package com.xy.lucky.auth.security.helper;

import com.xy.lucky.auth.security.config.RSAKeyProperties;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.exception.AuthenticationFailException;
import com.xy.lucky.security.util.RSAUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.PrivateKey;

/**
 * 加密解密助手类 - 支持密钥平滑过渡
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoHelper {

    private final RSAKeyProperties rsaKeyProperties;

    /**
     * 解密密文（支持新旧密钥平滑过渡）
     */
    public String decrypt(String encryptedText) {
        String normalized = encryptedText.replaceAll(" ", "+");

        // 优先使用当前密钥解密
        try {
            return RSAUtil.decrypt(normalized, rsaKeyProperties.getPrivateKeyStr());
        } catch (Exception e) {
            log.debug("当前密钥解密失败，尝试前一版本密钥");
        }

        // 尝试前一版本密钥（平滑过渡）
        PrivateKey previousKey = rsaKeyProperties.getPreviousPrivateKey();
        if (previousKey != null) {
            try {
                return RSAUtil.decryptWithKey(normalized, previousKey);
            } catch (Exception e) {
                log.warn("前一版本密钥解密也失败");
            }
        }

        throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
    }

    /**
     * 加密明文
     */
    public String encrypt(String plainText) {
        try {
            return RSAUtil.encrypt(plainText, rsaKeyProperties.getPublicKeyStr());
        } catch (Exception e) {
            log.error("RSA 加密失败", e);
            throw new AuthenticationFailException(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }
}