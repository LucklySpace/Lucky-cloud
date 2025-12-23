package com.xy.lucky.platform.utils;

import com.xy.lucky.platform.exception.UpdateException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

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
        String md5File = DigestUtils.md5Hex(file.getInputStream());
        if (!md5.equals(md5File)) {
            throw new UpdateException("文件MD5校验失败");
        }
        log.info("[文件MD5校验] 文件MD5校验成功, md5={}", md5);
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
