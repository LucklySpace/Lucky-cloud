package com.xy.lucky.file.util;

import com.xy.lucky.file.exception.FileException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

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

}
