package com.xy.lucky.security.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
public final class RSAUtil {

    public static final String KEY_ALGORITHM = "RSA";
    public static final String KEY_ALGORITHM_PADDING = "RSA/ECB/PKCS1Padding";
    public static final int RSA_KEY_SIZE = 2048;
    public static final String ENCODE_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private RSAUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    public static void generateKey(String publicKeyFilename, String privateKeyFilename, String secret, int keySize) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGenerator.initialize(Math.max(keySize, RSA_KEY_SIZE), new SecureRandom(secret.getBytes(StandardCharsets.UTF_8)));
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        writeFile(publicKeyFilename, Base64.getEncoder().encode(keyPair.getPublic().getEncoded()));
        writeFile(privateKeyFilename, Base64.getEncoder().encode(keyPair.getPrivate().getEncoded()));
    }

    public static PublicKey getPublicKey(String filename) throws Exception {
        return getPublicKey(readFile(filename));
    }

    public static PrivateKey getPrivateKey(String filename) throws Exception {
        return getPrivateKey(readFile(filename));
    }

    public static PublicKey getPublicKey(byte[] bytes) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(bytes));
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(keySpec);
    }

    public static PrivateKey getPrivateKey(byte[] bytes) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(bytes));
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(keySpec);
    }

    public static String encrypt(String plainText, String publicKeyStr) throws Exception {
        PublicKey publicKey = getPublicKey(publicKeyStr.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encryptText, String privateKeyStr) throws Exception {
        PrivateKey privateKey = getPrivateKey(privateKeyStr.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static String sign(PrivateKey privateKey, String plainText) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(ENCODE_ALGORITHM);
        byte[] hash = messageDigest.digest(plainText.getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(hash);

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verifySign(PublicKey publicKey, String plainText, String signedText) throws Exception {
        byte[] signedBytes = Base64.getDecoder().decode(signedText);

        MessageDigest messageDigest = MessageDigest.getInstance(ENCODE_ALGORITHM);
        byte[] hash = messageDigest.digest(plainText.getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(hash);

        return signature.verify(signedBytes);
    }

    private static byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    private static void writeFile(String filePath, byte[] bytes) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, bytes);
    }

    public static String bytesToHexString(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}

