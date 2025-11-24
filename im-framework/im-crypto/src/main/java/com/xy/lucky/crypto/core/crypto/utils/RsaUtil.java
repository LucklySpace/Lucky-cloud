//package com.xy.lucky.crypto.core.crypto.utils;
//
//import javax.crypto.Cipher;
//import java.security.KeyFactory;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.nio.charset.StandardCharsets;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;
//
/// **
// * RSA 加解密工具。
// * 使用 "RSA/ECB/PKCS1Padding" 变换，适用于短消息的非对称加密。
// */
//public class RsaUtil {
//
//
//    public static final String KEY_ALGORITHM = "RSA";
//    public static final String KEY_ALGORITHM_PADDING = "RSA/ECB/PKCS1Padding";
//
//    /**
//     * RSA 加密方法
//     *
//     * @param plainText 原始明文
//     * @param publicKeyStr 公钥字符串
//     * @return 加密后的字符串
//     */
//    public static String encrypt(String plainText, String publicKeyStr) throws Exception {
//        return performCipherOperation(Cipher.ENCRYPT_MODE, plainText, publicKeyStr.getBytes(), true);
//    }
//
//    /**
//     * RSA 解密方法
//     *
//     * @param encryptText 密文
//     * @param privateKeyStr 私钥字符串
//     * @return 解密后的明文
//     */
//    public static String decrypt(String encryptText, String privateKeyStr) throws Exception {
//        return performCipherOperation(Cipher.DECRYPT_MODE, encryptText, privateKeyStr.getBytes(), false);
//    }
//
//    /**
//     * 执行加解密操作
//     *
//     * @param mode 加解密模式
//     * @param inputText 输入文本
//     * @param keyBytes 密钥字节数组
//     * @param isPublicKey 是否为公钥
//     * @return 加密/解密结果
//     */
//    private static String performCipherOperation(int mode, String inputText, byte[] keyBytes, boolean isPublicKey) throws Exception {
//        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_PADDING);
//        if (isPublicKey) {
//            cipher.init(mode, getPublicKey(keyBytes));
//        } else {
//            cipher.init(mode, getPrivateKey(keyBytes));
//        }
//        byte[] outputBytes = cipher.doFinal(isPublicKey ? inputText.getBytes(StandardCharsets.UTF_8) : Base64.getDecoder().decode(inputText));
//        return isPublicKey ? Base64.getEncoder().encodeToString(outputBytes) : new String(outputBytes, StandardCharsets.UTF_8);
//    }
//
//    /**
//     * 获取公钥
//     *
//     * @param bytes 密钥字节数组
//     * @return 公钥
//     */
//    public static PublicKey getPublicKey(byte[] bytes) throws Exception {
//        return KeyFactory.getInstance(KEY_ALGORITHM)
//                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(bytes)));
//    }
//
//    /**
//     * 获取私钥
//     *
//     * @param bytes 密钥字节数组
//     * @return 私钥
//     */
//    public static PrivateKey getPrivateKey(byte[] bytes) throws Exception {
//        return KeyFactory.getInstance(KEY_ALGORITHM)
//                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(bytes)));
//    }
//
//
//
//}
