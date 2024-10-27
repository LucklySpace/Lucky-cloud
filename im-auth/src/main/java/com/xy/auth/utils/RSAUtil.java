package com.xy.auth.utils;


import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class RSAUtil {

    public static final String KEY_ALGORITHM = "RSA";
    public static final String KEY_ALGORITHM_PADDING = "RSA/ECB/PKCS1Padding";
    public static final int RSA_KEY_SIZE = 2048;
    public static final String ENCODE_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * 根据密文生成RSA公钥和私钥，并写入文件
     */
    public static void generateKey(String publicKeyFilename, String privateKeyFilename, String secret, int keySize) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGenerator.initialize(Math.max(keySize, RSA_KEY_SIZE), new SecureRandom(secret.getBytes()));
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
        return KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(bytes)));
    }

    public static PrivateKey getPrivateKey(byte[] bytes) throws Exception {
        return KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(bytes)));
    }

    public static String encrypt(String plainText, String publicKeyStr) throws Exception {
        return performCipherOperation(Cipher.ENCRYPT_MODE, plainText, publicKeyStr.getBytes(), true);
    }

    public static String decrypt(String encryptText, String privateKeyStr) throws Exception {
        return performCipherOperation(Cipher.DECRYPT_MODE, encryptText, privateKeyStr.getBytes(), false);
    }

    private static String performCipherOperation(int mode, String inputText, byte[] keyBytes, boolean isPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM_PADDING);
        if (isPublicKey) {
            cipher.init(mode, getPublicKey(keyBytes));
        } else {
            cipher.init(mode, getPrivateKey(keyBytes));
        }
        byte[] outputBytes = cipher.doFinal(isPublicKey ? inputText.getBytes(StandardCharsets.UTF_8) : Base64.getDecoder().decode(inputText));
        return isPublicKey ? Base64.getEncoder().encodeToString(outputBytes) : new String(outputBytes, StandardCharsets.UTF_8);
    }

    public static String sign(PrivateKey privateKey, String plainText) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance(ENCODE_ALGORITHM);
        byte[] outputDigest = messageDigest.digest(plainText.getBytes());

        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(outputDigest);

        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public static boolean verifySign(PublicKey publicKey, String plainText, String signed) throws Exception {
        byte[] signedArr = Base64.getDecoder().decode(signed);

        MessageDigest messageDigest = MessageDigest.getInstance(ENCODE_ALGORITHM);
        byte[] outputDigest = messageDigest.digest(plainText.getBytes());

        Signature verifySign = Signature.getInstance(SIGNATURE_ALGORITHM);
        verifySign.initVerify(publicKey);
        verifySign.update(outputDigest);

        return verifySign.verify(signedArr);
    }

    private static byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    private static void writeFile(String filePath, byte[] bytes) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, bytes);
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder sb = new StringBuilder();
        for (byte b : src) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}