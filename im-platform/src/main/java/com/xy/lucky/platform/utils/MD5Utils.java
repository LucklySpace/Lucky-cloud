package com.xy.lucky.platform.utils;

import com.xy.lucky.platform.exception.UpdateException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * MD5工具类
 */
@Slf4j
public class MD5Utils {

    /**
     * 校验MD5
     */
    @SneakyThrows
    public static void checkMD5(String md5, MultipartFile file) {
        String md5File = getMD5(file);
        if (!md5.equals(md5File)) {
            throw new UpdateException("文件MD5校验失败");
        }
        log.info("[文件MD5校验] 文件MD5校验成功, md5={}", md5);
    }

    /**
     * 计算MD5
     */
    @SneakyThrows
    public static String getMD5(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    @SneakyThrows
    public static void checkMD5(String md5, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            String md5File = getMD5(inputStream);
            if (!md5.equals(md5File)) {
                throw new UpdateException("文件MD5校验失败");
            }
            log.info("[文件MD5校验] 文件MD5校验成功, md5={}", md5);
        }
    }

    @SneakyThrows
    public static String getMD5(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return getMD5(inputStream);
        }
    }

    @SneakyThrows
    public static String getMD5(InputStream inputStream) {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 获取MD5
     */
    @SneakyThrows
    public static String md5Hex(String s) {
        return DigestUtils.md5Hex(s);
    }

    /**
     * 获取SHA256
     */
    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(b.length * 2);
            for (byte bt : b) sb.append(String.format("%02x", bt));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

}
