package com.xy.connect.utils;

import cn.hutool.crypto.digest.DigestUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.UUID;

public class MachineCodeUtils {
    public static final String WINDOWS = "Windows";
    public static final String LINUX = "Linux";

    // 缓存操作系统信息
    private static final String OS = System.getProperty("os.name").toLowerCase();

    // 缓存硬件信息
    private static String cachedMachineCode = null;

    public static String getMachineCode() {
        if (cachedMachineCode == null) {
            cachedMachineCode = getMachineCode(getOS());
        }
        return cachedMachineCode;
    }

    public static String getOS() {
        if (OS.contains("win")) {
            return WINDOWS;
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            return LINUX;
        } else if (OS.contains("mac")) {
            return "Mac OS";
        } else if (OS.contains("sunos")) {
            return "Solaris";
        } else {
            return "Unknown";
        }
    }

    public static String getMachineCode(String type) {
        if (Objects.isNull(type)) {
            return "";
        }
        Map<String, Object> codeMap = new HashMap<>();
        if (LINUX.equals(type)) {
            // 异步获取 BIOS 版本号和 UUID
            CompletableFuture<String> boisVersionFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getBoisVersion);
            CompletableFuture<String> uuidFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getUUID);

            try {
                codeMap.put("boisVersion", boisVersionFuture.get());
                codeMap.put("uuid", uuidFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("获取 Linux 信息失败", e);
            }

        } else if (WINDOWS.equals(type)) {
            // 异步获取 CPU 序列号和硬盘序列号
            CompletableFuture<String> processorIdFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getCPUSerialNumber);
            CompletableFuture<String> serialNumberFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getHardDiskSerialNumber);

            try {
                codeMap.put("ProcessorId", processorIdFuture.get());
                codeMap.put("SerialNumber", serialNumberFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("获取 Windows 信息失败", e);
            }

        } else {
            return "";
        }

        // 添加随机UUID生成器
        codeMap.put("randomUUID", UUID.randomUUID().toString());

        String codeMapStr = JsonUtil.toJSONString(codeMap);
        String serials = DigestUtil.md5Hex(codeMapStr);
        return getSplitString(serials, "-", 4).toUpperCase();
    }

    public static String getSplitString(String str, String joiner, int number) {
        StringBuilder sb = new StringBuilder();
        int len = str.length();
        for (int i = 0; i < len; i += number) {
            if (i + number <= len) {
                sb.append(str, i, i + number);
            } else {
                sb.append(str.substring(i));
            }
            if (i + number < len) {
                sb.append(joiner);
            }
        }
        return sb.toString();
    }

    public static String getCPUSerialNumber() {
        return executeCommand(new String[]{"wmic", "cpu", "get", "ProcessorId"});
    }

    public static String getHardDiskSerialNumber() {
        return executeCommand(new String[]{"wmic", "path", "win32_physicalmedia", "get", "serialnumber"});
    }

    public static String getUUID() {
        return executeCommand(new String[]{"sudo", "dmidecode", "-s", "system-uuid"});
    }

    public static String getBoisVersion() {
        return executeCommand(new String[]{"sudo", "dmidecode", "-s", "bios-version"});
    }

    /**
     * 通用的命令执行方法
     *
     * @param command 系统命令
     * @return 命令输出
     */
    public static String executeCommand(String[] command) {
        StringBuilder result = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (InputStream in = process.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line.trim());
                    break;
                }
            }
            process.destroy();
        } catch (IOException e) {
            throw new RuntimeException("执行命令失败: " + String.join(" ", command), e);
        }
        return result.toString();
    }
}
