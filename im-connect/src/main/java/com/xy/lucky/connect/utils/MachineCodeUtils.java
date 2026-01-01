package com.xy.lucky.connect.utils;

import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Machine code utility class for generating unique machine identifiers.
 * Uses OSHI library for cross-platform hardware information retrieval.
 */
@Slf4j
public class MachineCodeUtils {
    public static final String WINDOWS = "Windows";
    public static final String LINUX = "Linux";
    public static final String MAC_OS = "Mac OS";
    public static final String SOLARIS = "Solaris";
    public static final String UNKNOWN = "Unknown";

    // 缓存操作系统信息
    private static final String OS = System.getProperty("os.name").toLowerCase();

    // 缓存硬件信息（使用 volatile 保证可见性）
    private static volatile String cachedMachineCode = null;

    // 用于双重检查锁定的锁对象
    private static final Object LOCK = new Object();

    // 延迟初始化 OSHI 对象（避免类加载时失败）
    private static volatile HardwareAbstractionLayer hardware = null;

    /**
     * Get or initialize the hardware abstraction layer
     */
    private static HardwareAbstractionLayer getHardware() {
        if (hardware == null) {
            synchronized (LOCK) {
                if (hardware == null) {
                    try {
                        SystemInfo systemInfo = new SystemInfo();
                        hardware = systemInfo.getHardware();
                    } catch (Exception e) {
                        log.error("Failed to initialize OSHI SystemInfo", e);
                        // Return null, let caller handle it
                    }
                }
            }
        }
        return hardware;
    }

    /**
     * Get the cached machine code or generate a new one
     */
    public static String getMachineCode() {
        if (cachedMachineCode == null) {
            synchronized (LOCK) {
                if (cachedMachineCode == null) {
                    cachedMachineCode = generateMachineCode(getOS());
                }
            }
        }
        return cachedMachineCode;
    }

    /**
     * Detect the current operating system type
     */
    public static String getOS() {
        if (OS.contains("win")) {
            return WINDOWS;
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            return LINUX;
        } else if (OS.contains("mac")) {
            return MAC_OS;
        } else if (OS.contains("sunos")) {
            return SOLARIS;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Generate machine code for a specific OS type
     *
     * @param type the operating system type
     * @return the generated machine code
     */
    public static String getMachineCode(String type) {
        return generateMachineCode(type);
    }

    /**
     * Internal method to generate machine code
     */
    private static String generateMachineCode(String type) {
        if (Objects.isNull(type)) {
            return "";
        }

        Map<String, Object> codeMap = new HashMap<>();

        switch (type) {
            case LINUX:
                // 异步获取 BIOS 版本号和 UUID
                CompletableFuture<String> linuxBiosFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getBiosVersion);
                CompletableFuture<String> linuxUuidFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getUUID);

                codeMap.put("biosVersion", safeGet(linuxBiosFuture, "linux-bios"));
                codeMap.put("uuid", safeGet(linuxUuidFuture, "linux-uuid"));
                break;

            case WINDOWS:
                // 异步获取 CPU 序列号和硬盘序列号
                CompletableFuture<String> winCpuFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getCPUSerialNumber);
                CompletableFuture<String> winDiskFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getHardDiskSerialNumber);

                codeMap.put("ProcessorId", safeGet(winCpuFuture, "windows-cpu"));
                codeMap.put("SerialNumber", safeGet(winDiskFuture, "windows-disk"));
                break;

            case MAC_OS:
                // 异步获取 Mac OS 硬件信息
                CompletableFuture<String> macCpuFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getCPUSerialNumber);
                CompletableFuture<String> macUuidFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getUUID);

                codeMap.put("ProcessorId", safeGet(macCpuFuture, "macos-cpu"));
                codeMap.put("uuid", safeGet(macUuidFuture, "macos-uuid"));
                break;

            case SOLARIS:
            case UNKNOWN:
            default:
                // 对于未知或其他系统，使用通用方式获取信息
                CompletableFuture<String> cpuFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getCPUSerialNumber);
                CompletableFuture<String> uuidFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getUUID);

                codeMap.put("ProcessorId", safeGet(cpuFuture, "generic-cpu"));
                codeMap.put("uuid", safeGet(uuidFuture, "generic-uuid"));
                codeMap.put("osType", type);
                break;
        }

        // 添加稳定的UUID（基于时间戳，每次机器码生成后固定）
        codeMap.put("instanceId", UUID.randomUUID().toString());

        String codeMapStr = JacksonUtil.toJSONString(codeMap);
        String serials = DigestUtil.md5Hex(codeMapStr);
        return getSplitString(serials, "-", 4).toUpperCase();
    }

    /**
     * Safely get the result from a CompletableFuture with fallback
     */
    private static String safeGet(CompletableFuture<String> future, String fallbackPrefix) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            log.error("Interrupted while getting hardware info", e);
            Thread.currentThread().interrupt();
            return getFallbackValue(fallbackPrefix);
        } catch (Exception e) {
            log.error("Failed to get hardware info", e);
            return getFallbackValue(fallbackPrefix);
        }
    }

    /**
     * Split a string into chunks with a joiner
     */
    public static String getSplitString(String str, String joiner, int number) {
        if (str == null || str.isEmpty()) {
            return "";
        }
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

    /**
     * Get CPU serial number / processor ID
     */
    public static String getCPUSerialNumber() {
        try {
            HardwareAbstractionLayer hal = getHardware();
            if (hal == null) {
                log.warn("Hardware layer not available, using fallback for CPU serial");
                return getFallbackValue("cpu-serial");
            }

            CentralProcessor processor = hal.getProcessor();
            String processorId = processor.getProcessorIdentifier().getProcessorID();
            if (processorId != null && !processorId.isEmpty()) {
                return processorId;
            }
            log.warn("CPU SerialNumber is empty, using fallback");
            return getFallbackValue("cpu-serial");
        } catch (Exception e) {
            log.error("Failed to get CPU serial number", e);
            return getFallbackValue("cpu-serial");
        }
    }

    /**
     * Get hard disk serial number (tries all disks if first one fails)
     */
    public static String getHardDiskSerialNumber() {
        try {
            HardwareAbstractionLayer hal = getHardware();
            if (hal == null) {
                log.warn("Hardware layer not available, using fallback for disk serial");
                return getFallbackValue("disk-serial");
            }

            List<HWDiskStore> diskStores = hal.getDiskStores();
            if (diskStores != null && !diskStores.isEmpty()) {
                // 尝试遍历所有硬盘，找到第一个有效的序列号
                for (HWDiskStore disk : diskStores) {
                    String serial = disk.getSerial();
                    if (serial != null && !serial.isEmpty() && !"unknown".equalsIgnoreCase(serial)) {
                        return serial;
                    }
                }
            }
            log.warn("Hard disk serial number is empty or all unknown, using fallback");
            return getFallbackValue("disk-serial");
        } catch (Exception e) {
            log.error("Failed to get hard disk serial number", e);
            return getFallbackValue("disk-serial");
        }
    }

    /**
     * Get system hardware UUID
     */
    public static String getUUID() {
        try {
            HardwareAbstractionLayer hal = getHardware();
            if (hal == null) {
                log.warn("Hardware layer not available, using fallback for UUID");
                return getFallbackValue("system-uuid");
            }

            ComputerSystem computerSystem = hal.getComputerSystem();
            String uuid = computerSystem.getHardwareUUID();
            if (uuid != null && !uuid.isEmpty() && !"unknown".equalsIgnoreCase(uuid)) {
                return uuid;
            }
            log.warn("Hardware UUID is empty, using fallback");
            return getFallbackValue("system-uuid");
        } catch (Exception e) {
            log.error("Failed to get system UUID", e);
            return getFallbackValue("system-uuid");
        }
    }

    /**
     * Get BIOS/Firmware version
     */
    public static String getBiosVersion() {
        try {
            HardwareAbstractionLayer hal = getHardware();
            if (hal == null) {
                log.warn("Hardware layer not available, using fallback for BIOS version");
                return getFallbackValue("bios-version");
            }

            ComputerSystem computerSystem = hal.getComputerSystem();
            String biosVersion = computerSystem.getFirmware().getVersion();
            if (biosVersion != null && !biosVersion.isEmpty() && !"unknown".equalsIgnoreCase(biosVersion)) {
                return biosVersion;
            }
            log.warn("BIOS version is empty, using fallback");
            return getFallbackValue("bios-version");
        } catch (Exception e) {
            log.error("Failed to get BIOS version", e);
            return getFallbackValue("bios-version");
        }
    }

    /**
     * Fallback value generator for hardware information.
     * Generates a stable value based on system properties.
     *
     * @param prefix prefix for the fallback value
     * @return a stable fallback value based on system properties
     */
    private static String getFallbackValue(String prefix) {
        try {
            // 使用系统属性组合生成降级值
            String osName = System.getProperty("os.name", "unknown");
            String osVersion = System.getProperty("os.version", "unknown");
            String osArch = System.getProperty("os.arch", "unknown");
            String userName = System.getProperty("user.name", "unknown");
            String javaHome = System.getProperty("java.home", "unknown");
            String userHome = System.getProperty("user.home", "unknown");

            // 组合信息生成一个相对稳定的标识
            String combined = prefix + "-" + osName + "-" + osVersion + "-" + osArch
                    + "-" + userName + "-" + javaHome.hashCode() + "-" + userHome.hashCode();
            return DigestUtil.md5Hex(combined).substring(0, 16);
        } catch (Exception e) {
            log.error("Failed to generate fallback value", e);
            return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
