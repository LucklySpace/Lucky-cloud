package com.xy.lucky.oss.util;

import com.xy.lucky.oss.exception.FileException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * MD5工具类
 */
@Slf4j
public class MD5Utils {

    @SneakyThrows
    public static void checkMD5(String md5, MultipartFile file) {
        String md5File = DigestUtils.md5Hex(file.getInputStream());
        if (!md5.equals(md5File)) {
            throw new FileException("文件MD5校验失败");
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

}
