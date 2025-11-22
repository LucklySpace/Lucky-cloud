package com.xy.lucky.crypto.core.utils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyUtil {

    public static PublicKey getPublicKey(String base64PublicKey) throws Exception {
        String pem = base64PublicKey.replaceAll("-----\\w+ PUBLIC KEY-----", "").replaceAll("\s", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static PrivateKey getPrivateKey(String base64PrivateKey) throws Exception {
        String pem = base64PrivateKey.replaceAll("-----\\w+ PRIVATE KEY-----", "").replaceAll("\s", "");
        byte[] bytes = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }
}