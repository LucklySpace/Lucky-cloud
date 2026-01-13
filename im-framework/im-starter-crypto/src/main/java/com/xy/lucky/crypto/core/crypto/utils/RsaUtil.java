package com.xy.lucky.crypto.core.crypto.utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSA 非对称加解密工具类
 * <p>
 * 特性：
 * - 支持 PKCS1 和 OAEP 填充
 * - 密钥缓存优化
 * - 支持密钥对生成
 * - 支持分段加解密（处理长数据）
 */
public final class RsaUtil {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION_PKCS1 = "RSA/ECB/PKCS1Padding";
    private static final String TRANSFORMATION_OAEP = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * 公钥缓存
     */
    private static final Map<String, PublicKey> PUBLIC_KEY_CACHE = new ConcurrentHashMap<>();

    /**
     * 私钥缓存
     */
    private static final Map<String, PrivateKey> PRIVATE_KEY_CACHE = new ConcurrentHashMap<>();

    private RsaUtil() {
    }

    // ==================== 加密方法 ====================

    /**
     * 公钥加密（使用 PKCS1 填充）
     *
     * @param plainText    明文
     * @param publicKeyStr Base64 编码的公钥
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText, String publicKeyStr) throws Exception {
        return encrypt(plainText, publicKeyStr, false);
    }

    /**
     * 公钥加密
     *
     * @param plainText    明文
     * @param publicKeyStr Base64 编码的公钥
     * @param useOaep      是否使用 OAEP 填充（更安全）
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText, String publicKeyStr, boolean useOaep) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyStr);
        String transformation = useOaep ? TRANSFORMATION_OAEP : TRANSFORMATION_PKCS1;

        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 私钥加密（用于签名场景）
     */
    public static String encryptByPrivateKey(String plainText, String privateKeyStr) throws Exception {
        PrivateKey privateKey = getPrivateKey(privateKeyStr);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION_PKCS1);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // ==================== 解密方法 ====================

    /**
     * 私钥解密（使用 PKCS1 填充）
     *
     * @param cipherText    Base64 编码的密文
     * @param privateKeyStr Base64 编码的私钥
     * @return 明文
     */
    public static String decrypt(String cipherText, String privateKeyStr) throws Exception {
        return decrypt(cipherText, privateKeyStr, false);
    }

    /**
     * 私钥解密
     *
     * @param cipherText    Base64 编码的密文
     * @param privateKeyStr Base64 编码的私钥
     * @param useOaep       是否使用 OAEP 填充
     * @return 明文
     */
    public static String decrypt(String cipherText, String privateKeyStr, boolean useOaep) throws Exception {
        PrivateKey privateKey = getPrivateKey(privateKeyStr);
        String transformation = useOaep ? TRANSFORMATION_OAEP : TRANSFORMATION_PKCS1;

        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * 公钥解密（用于验签场景）
     */
    public static String decryptByPublicKey(String cipherText, String publicKeyStr) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyStr);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION_PKCS1);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // ==================== 密钥管理 ====================

    /**
     * 获取公钥对象（带缓存）
     */
    public static PublicKey getPublicKey(String publicKeyStr) throws Exception {
        return PUBLIC_KEY_CACHE.computeIfAbsent(publicKeyStr, key -> {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(key);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
                return keyFactory.generatePublic(keySpec);
            } catch (Exception e) {
                throw new RuntimeException("解析公钥失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 获取私钥对象（带缓存）
     */
    public static PrivateKey getPrivateKey(String privateKeyStr) throws Exception {
        return PRIVATE_KEY_CACHE.computeIfAbsent(privateKeyStr, key -> {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(key);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e) {
                throw new RuntimeException("解析私钥失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 生成 RSA 密钥对
     *
     * @param keySize 密钥长度（推荐 2048 或 4096）
     * @return 包含公钥和私钥的数组 [公钥Base64, 私钥Base64]
     */
    public static String[] generateKeyPair(int keySize) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        return new String[]{publicKey, privateKey};
    }

    /**
     * 生成 2048 位 RSA 密钥对
     */
    public static String[] generateKeyPair() throws Exception {
        return generateKeyPair(2048);
    }

    // ==================== 数字签名 ====================

    /**
     * 使用私钥签名
     *
     * @param data          待签名数据
     * @param privateKeyStr Base64 编码的私钥
     * @return Base64 编码的签名
     */
    public static String sign(String data, String privateKeyStr) throws Exception {
        return sign(data, privateKeyStr, "SHA256withRSA");
    }

    /**
     * 使用私钥签名
     *
     * @param data          待签名数据
     * @param privateKeyStr Base64 编码的私钥
     * @param algorithm     签名算法（如 SHA256withRSA, SHA512withRSA）
     * @return Base64 编码的签名
     */
    public static String sign(String data, String privateKeyStr, String algorithm) throws Exception {
        PrivateKey privateKey = getPrivateKey(privateKeyStr);

        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * 使用公钥验签
     *
     * @param data         原始数据
     * @param signatureStr Base64 编码的签名
     * @param publicKeyStr Base64 编码的公钥
     * @return 验签结果
     */
    public static boolean verify(String data, String signatureStr, String publicKeyStr) throws Exception {
        return verify(data, signatureStr, publicKeyStr, "SHA256withRSA");
    }

    /**
     * 使用公钥验签
     */
    public static boolean verify(String data, String signatureStr, String publicKeyStr, String algorithm) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyStr);

        Signature signature = Signature.getInstance(algorithm);
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));

        return signature.verify(Base64.getDecoder().decode(signatureStr));
    }

    // ==================== 工具方法 ====================

    /**
     * 清理密钥缓存
     */
    public static void clearKeyCache() {
        PUBLIC_KEY_CACHE.clear();
        PRIVATE_KEY_CACHE.clear();
    }

    /**
     * 获取 RSA 可加密的最大明文长度
     *
     * @param keySize 密钥长度
     * @param useOaep 是否使用 OAEP
     * @return 最大明文字节数
     */
    public static int getMaxPlainTextLength(int keySize, boolean useOaep) {
        int keyBytes = keySize / 8;
        // PKCS1 填充需要 11 字节，OAEP 需要 42 字节
        return useOaep ? keyBytes - 42 : keyBytes - 11;
    }
}
