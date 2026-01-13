package com.xy.lucky.crypto.core.crypto.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高性能 AES 加解密工具类
 * <p>
 * 特性：
 * - 支持 ECB/CBC/GCM 三种模式
 * - GCM 模式提供认证加密，推荐使用
 * - SecretKey 缓存优化，避免重复创建
 * - ThreadLocal Cipher 复用（ECB/CBC模式）
 * - 安全随机数生成 IV/Nonce
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH = 12;  // GCM推荐 12 bytes
    private static final int GCM_TAG_LENGTH = 128; // 认证标签长度 128 bits
    private static final int CBC_IV_LENGTH = 16;   // CBC 需要 16 bytes IV

    /**
     * SecretKey 缓存，避免重复创建
     */
    private static final Map<String, SecretKeySpec> KEY_CACHE = new ConcurrentHashMap<>();

    /**
     * ThreadLocal Cipher 缓存 (ECB模式)
     */
    private static final ThreadLocal<Cipher> ECB_CIPHER_ENCRYPT = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance("AES/ECB/PKCS5Padding");
        } catch (Exception e) {
            throw new RuntimeException("初始化 AES/ECB Cipher 失败", e);
        }
    });

    private static final ThreadLocal<Cipher> ECB_CIPHER_DECRYPT = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance("AES/ECB/PKCS5Padding");
        } catch (Exception e) {
            throw new RuntimeException("初始化 AES/ECB Cipher 失败", e);
        }
    });

    /**
     * 安全随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesUtil() {
    }

    // ==================== GCM 模式（推荐） ====================

    /**
     * GCM 模式加密（推荐）
     * 输出格式: Base64(IV + 密文 + AuthTag)
     *
     * @param plainText 明文
     * @param key       密钥(16/24/32字节)
     * @return Base64编码的密文
     */
    public static String encryptGcm(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = getOrCreateKey(key);

        // 生成随机 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 组合 IV + 密文
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * GCM 模式解密
     *
     * @param cipherText Base64编码的密文(包含IV)
     * @param key        密钥
     * @return 明文
     */
    public static String decryptGcm(String cipherText, String key) throws Exception {
        SecretKeySpec secretKey = getOrCreateKey(key);

        byte[] decoded = Base64.getDecoder().decode(cipherText);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

        // 提取 IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        // 提取密文
        byte[] cipherBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherBytes);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }

    // ==================== CBC 模式 ====================

    /**
     * CBC 模式加密
     * 输出格式: Base64(IV + 密文)
     */
    public static String encryptCbc(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = getOrCreateKey(key);

        byte[] iv = new byte[CBC_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherBytes);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * CBC 模式解密
     */
    public static String decryptCbc(String cipherText, String key) throws Exception {
        SecretKeySpec secretKey = getOrCreateKey(key);

        byte[] decoded = Base64.getDecoder().decode(cipherText);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

        byte[] iv = new byte[CBC_IV_LENGTH];
        byteBuffer.get(iv);

        byte[] cipherBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }

    // ==================== ECB 模式（高性能，使用缓存） ====================

    /**
     * ECB 模式加密（使用 ThreadLocal 缓存的 Cipher）
     * 注意：ECB 模式安全性较低，仅用于兼容旧系统
     */
    public static String encryptEcb(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = getOrCreateKey(key);
        Cipher cipher = ECB_CIPHER_ENCRYPT.get();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * ECB 模式解密
     */
    public static String decryptEcb(String cipherText, String key) throws Exception {
        SecretKeySpec secretKey = getOrCreateKey(key);
        Cipher cipher = ECB_CIPHER_DECRYPT.get();
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
    }

    // ==================== 兼容旧接口 ====================

    /**
     * 加密（默认使用 GCM 模式）
     */
    public static String encrypt(String data, String key) throws Exception {
        return encryptGcm(data, key);
    }

    /**
     * 解密（默认使用 GCM 模式）
     */
    public static String decrypt(String data, String key) throws Exception {
        return decryptGcm(data, key);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取或创建 SecretKey（带缓存）
     */
    private static SecretKeySpec getOrCreateKey(String key) {
        return KEY_CACHE.computeIfAbsent(key, k -> {
            validateKey(k);
            return new SecretKeySpec(k.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        });
    }

    /**
     * 校验密钥长度
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

    /**
     * 生成随机 AES 密钥
     *
     * @param keySize 密钥位数(128/192/256)
     * @return Base64编码的密钥
     */
    public static String generateKey(int keySize) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(keySize, SECURE_RANDOM);
        SecretKey secretKey = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * 生成随机 16 字节密钥字符串
     */
    public static String generateKeyString(int byteLength) {
        byte[] keyBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(keyBytes);
        // 转换为可打印字符
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (byte b : keyBytes) {
            sb.append(chars.charAt(Math.abs(b) % chars.length()));
        }
        return sb.toString();
    }

    /**
     * 清理密钥缓存（用于密钥轮换场景）
     */
    public static void clearKeyCache() {
        KEY_CACHE.clear();
    }

    /**
     * 从缓存中移除指定密钥
     */
    public static void removeKeyFromCache(String key) {
        KEY_CACHE.remove(key);
    }
}
