package com.xy.lucky.crypto.core.crypto.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加解密工具。
 * 默认采用 "AES/ECB/PKCS5Padding"，如需更强安全性可替换为 CBC/GCM 模式并引入 IV/Nonce。
 */
public class AesUtil {

    /**
     * Cipher 变换常量。
     */
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String ALGORITHM = "AES";

    /**
     * 加密
     *
     * @param data 数据
     * @param key  密钥
     * @return 加密内容
     * @throws Exception
     */
    public static String encrypt(String data, String key) throws Exception {
        validateKey(key);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 解密
     *
     * @param data 加密内容
     * @param key  密钥
     * @return 数据
     * @throws Exception
     */
    public static String decrypt(String data, String key) throws Exception {
        validateKey(key);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        return new String(cipher.doFinal(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
    }

    /**
     * 校验密钥长度，支持 16/24/32 字节（128/192/256 位）。
     */
    private static void validateKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("AES 密钥不可为 null");
        }
        int len = key.getBytes(StandardCharsets.UTF_8).length;
        if (len != 16 && len != 24 && len != 32) {
            throw new IllegalArgumentException("AES 密钥长度必须为 16/24/32 字节，当前为: " + len);
        }
    }
}
