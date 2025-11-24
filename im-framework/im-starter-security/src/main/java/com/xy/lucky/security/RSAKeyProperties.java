package com.xy.lucky.security;

import com.xy.lucky.security.util.RSAUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Data
@Slf4j
@ConfigurationProperties(prefix = "security.rsa.key")
public class RSAKeyProperties {

    private static final int KEY_SIZE = 2048;
    private static final String RSA_DIR = "./rsa/";

    private String secret;
    private String publicKeyStr;
    private String privateKeyStr;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @PostConstruct
    public void generateKeyPair() {
        try {
            String publicKeyPath = RSA_DIR + "publicKey.pem";
            String privateKeyPath = RSA_DIR + "privateKey.pem";

            File rsaDirectory = new File(RSA_DIR);
            if (!rsaDirectory.exists()) {
                rsaDirectory.mkdirs();
            }

            RSAUtil.generateKey(publicKeyPath, privateKeyPath, secret, KEY_SIZE);

            this.publicKey = RSAUtil.getPublicKey(publicKeyPath);
            this.privateKey = RSAUtil.getPrivateKey(privateKeyPath);

            this.publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            this.privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            log.info("RSA key pair generated at rsa/");
        } catch (Exception e) {
            log.error("Failed to generate RSA key pair: {}", e.getMessage());
        }
    }
}

