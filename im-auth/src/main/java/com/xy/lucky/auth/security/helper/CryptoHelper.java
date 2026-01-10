package com.xy.lucky.auth.security.helper;

import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.RSAKeyProperties;
import com.xy.lucky.security.exception.AuthenticationFailException;
import com.xy.lucky.security.util.RSAUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 加密解密助手类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoHelper {

    private final RSAKeyProperties rsaKeyProperties;

    /**
     * 解密密文（RSA私钥解密）
     *
     * @param encryptedText 加密的文本
     * @return 解密后的明文
     * @throws AuthenticationFailException 解密失败时抛出
     */
    public String decrypt(String encryptedText) {
        try {
            // 处理 URL 传输中 '+' 被转义为空格的问题
            String normalized = encryptedText.replaceAll(" ", "+");
            return RSAUtil.decrypt(normalized, rsaKeyProperties.getPrivateKeyStr());
        } catch (Exception e) {
            log.error("RSA 解密失败", e);
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * 加密明文（RSA公钥加密）
     *
     * @param plainText 明文
     * @return 加密后的密文
     * @throws AuthenticationFailException 加密失败时抛出
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

