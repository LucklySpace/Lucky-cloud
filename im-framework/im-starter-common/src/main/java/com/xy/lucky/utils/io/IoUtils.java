package com.xy.lucky.utils.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * IO 工具类
 */
public class IoUtils {

    /**
     * 从流中读取 UTF8 编码的内容
     *
     * @param in      输入流
     * @param isClose 是否关闭
     * @return 内容
     * @throws IOException IO 异常
     */
    public static String readUtf8(InputStream in, boolean isClose) throws IOException {
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        } finally {
            if (isClose && in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // 静默关闭
                }
            }
        }
    }

}