package com.xy.lucky.security;

import com.xy.lucky.security.util.RSAUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

@Data
@Slf4j
@ConfigurationProperties(prefix = "security.rsa.key")
public class RSAKeyProperties {

    private static final int KEY_SIZE = 2048;
    private static final String RSA_DIR = "./rsa/";
    private static final String PUBLIC_KEY_FILE = "publicKey.pem";
    private static final String PRIVATE_KEY_FILE = "privateKey.pem";

    private String secret;
    private String publicKeyStr;
    private String privateKeyStr;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void generateKeyPair() {
        try {
            Path rsaDirectory = Paths.get(RSA_DIR);
            if (!Files.exists(rsaDirectory)) {
                Files.createDirectories(rsaDirectory);
            }

            String publicKeyPath = RSA_DIR + PUBLIC_KEY_FILE;
            String privateKeyPath = RSA_DIR + PRIVATE_KEY_FILE;

            RSAUtil.generateKey(publicKeyPath, privateKeyPath, secret, KEY_SIZE);

            this.publicKey = RSAUtil.getPublicKey(publicKeyPath);
            this.privateKey = RSAUtil.getPrivateKey(privateKeyPath);

            this.publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            this.privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            log.info("RSA 密钥对已生成，路径: {}", RSA_DIR);
        } catch (IOException e) {
            log.error("创建 RSA 目录失败", e);
        } catch (Exception e) {
            log.error("生成 RSA 密钥对失败", e);
        }
    }
}

