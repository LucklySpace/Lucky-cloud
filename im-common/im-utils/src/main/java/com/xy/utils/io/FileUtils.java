package com.xy.utils.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * 文件工具类
 */
public class FileUtils {

    /**
     * 创建临时文件
     * 该文件会在 JVM 退出时，进行删除
     *
     * @param data 文件内容
     * @return 文件
     */
    public static File createTempFile(String data) throws IOException {
        File file = createTempFile();
        // 写入内容
        Files.write(file.toPath(), data.getBytes(), StandardOpenOption.WRITE);
        return file;
    }

    /**
     * 创建临时文件
     * 该文件会在 JVM 退出时，进行删除
     *
     * @param data 文件内容
     * @return 文件
     */
    public static File createTempFile(byte[] data) throws IOException {
        File file = createTempFile();
        // 写入内容
        Files.write(file.toPath(), data, StandardOpenOption.WRITE);
        return file;
    }

    /**
     * 创建临时文件，无内容
     * 该文件会在 JVM 退出时，进行删除
     *
     * @return 文件
     */
    public static File createTempFile() throws IOException {
        // 创建文件，通过 UUID 保证唯一
        File file = File.createTempFile(UUID.randomUUID().toString(), null);
        // 标记 JVM 退出时，自动删除
        file.deleteOnExit();
        return file;
    }

}