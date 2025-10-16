package com.xy.auth.security;

import com.xy.auth.utils.RSAUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * rsa参数类
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "security.rsa.key")
public class IMRSAKeyProperties {

    // 指定密钥长度
    private static final int KEY_SIZE = 2048;
    // 设置相对目录 rsa
    private static final String RSA_DIR = "./rsa/";

    // 盐值
    private String secret;
    // 公钥字符串
    private String publicKeyStr;
    //私钥字符串
    private String privateKeyStr;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    /**
     * 在构造完成后生成公钥和私钥
     */
    @PostConstruct
    public void generateKeyPair() {
        try {
            // 定义公钥和私钥的文件路径（相对于项目根目录）
            String publicKeyPath = RSA_DIR + "publicKey.pem";
            String privateKeyPath = RSA_DIR + "privateKey.pem";

            // 创建 rsa 目录
            File rsaDirectory = new File(RSA_DIR);
            if (!rsaDirectory.exists()) {
                rsaDirectory.mkdirs();
            }

            // 调用 RSAUtil 生成 RSA 密钥对
            RSAUtil.generateKey(publicKeyPath, privateKeyPath, secret, KEY_SIZE);

            // 从生成的文件中读取公钥和私钥，并注入到类的属性中
            this.publicKey = RSAUtil.getPublicKey(publicKeyPath);
            this.privateKey = RSAUtil.getPrivateKey(privateKeyPath);

            this.publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            this.privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            log.info("RSA 公钥和私钥生成成功，保存于相对路径 rsa/");
        } catch (Exception e) {
            log.error("RSA 密钥生成失败: {}", e.getMessage());
        }
    }

}