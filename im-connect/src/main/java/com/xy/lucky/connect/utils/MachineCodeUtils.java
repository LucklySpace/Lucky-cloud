package com.xy.lucky.connect.utils;


import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.alibaba.nacos.common.utils.MD5Utils.md5Hex;

public class MachineCodeUtils {
    public static final String WINDOWS = "Windows";
    public static final String LINUX = "Linux";
    public static final String MAC_OS = "Mac OS";

    // Cache OS info
    private static final String OS = System.getProperty("os.name").toLowerCase();

    // Cache hardware info
    private static String cachedMachineCode = null;

    public static String getMachineCode() throws NoSuchAlgorithmException {
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
            return MAC_OS;
        } else {
            return "Unknown";
        }
    }

    public static String getMachineCode(String type) throws NoSuchAlgorithmException {
        if (Objects.isNull(type)) {
            return "";
        }
        Map<String, Object> codeMap = new HashMap<>();

        switch (type) {
            case LINUX:
                // Async get BIOS version and UUID
                CompletableFuture<String> biosVersionFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getLinuxBiosVersion);
                CompletableFuture<String> uuidFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getLinuxUUID);
                try {
                    codeMap.put("biosVersion", biosVersionFuture.get());
                    codeMap.put("uuid", uuidFuture.get());
                } catch (InterruptedException | ExecutionException e) {
                    // Fallback to general hardware info
                    codeMap.putAll(getFallbackHardwareInfo());
                }
                break;

            case WINDOWS:
                // Async get CPU info and disk info
                CompletableFuture<String> cpuInfoFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getWindowsCPUInfo);
                CompletableFuture<String> diskInfoFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getWindowsDiskInfo);

                try {
                    codeMap.put("ProcessorId", cpuInfoFuture.get());
                    codeMap.put("SerialNumber", diskInfoFuture.get());
                } catch (InterruptedException | ExecutionException e) {
                    // Fallback to general hardware info
                    codeMap.putAll(getFallbackHardwareInfo());
                }
                break;

            case MAC_OS:
                // Async get Mac hardware UUID and serial number
                CompletableFuture<String> hardwareUUIDFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getMacHardwareUUID);
                CompletableFuture<String> serialNumberFuture = CompletableFuture.supplyAsync(MachineCodeUtils::getMacSerialNumber);

                try {
                    codeMap.put("hardwareUUID", hardwareUUIDFuture.get());
                    codeMap.put("serialNumber", serialNumberFuture.get());
                } catch (InterruptedException | ExecutionException e) {
                    // Fallback to general hardware info
                    codeMap.putAll(getFallbackHardwareInfo());
                }
                break;

            default:
                // Unknown OS, use fallback
                codeMap.putAll(getFallbackHardwareInfo());
                break;

        }

        // Add random UUID generator for uniqueness
        codeMap.put("randomUUID", UUID.randomUUID().toString());

        String codeMapStr = JacksonUtil.toJSONString(codeMap);
        String serials = md5Hex(codeMapStr.getBytes(StandardCharsets.UTF_8));
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

    // ==================== Fallback Hardware Info ====================

    /**
     * Get fallback hardware info using MAC address and system properties
     * This is used when platform-specific methods fail
     *
     * @return Map of hardware identifiers
     */
    private static Map<String, String> getFallbackHardwareInfo() {
        Map<String, String> info = new HashMap<>();

        // Get MAC address
        String macAddress = getMacAddress();
        if (macAddress != null && !macAddress.isEmpty()) {
            info.put("macAddress", macAddress);
        }

        // Get system properties
        info.put("osArch", System.getProperty("os.arch", ""));
        info.put("osVersion", System.getProperty("os.version", ""));
        info.put("userName", System.getProperty("user.name", ""));
        info.put("userHome", System.getProperty("user.home", ""));
        info.put("javaIoTmpdir", System.getProperty("java.io.tmpdir", ""));

        // Get runtime info
        info.put("availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors()));

        // Get file system root info
        File[] roots = File.listRoots();
        if (roots != null && roots.length > 0) {
            StringBuilder rootsInfo = new StringBuilder();
            for (File root : roots) {
                rootsInfo.append(root.getAbsolutePath())
                        .append(":")
                        .append(root.getTotalSpace())
                        .append(";");
            }
            info.put("fileSystemRoots", rootsInfo.toString());
        }

        // Get JVM runtime info
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        info.put("vmName", runtimeMXBean.getVmName());
        info.put("vmVendor", runtimeMXBean.getVmVendor());

        return info;
    }

    /**
     * Get the first available MAC address from network interfaces
     *
     * @return MAC address as hex string, or empty string if not available
     */
    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) {
                return "";
            }

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                // Skip loopback and inactive interfaces
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }

                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1) {
                            sb.append(":");
                        }
                    }
                    return sb.toString();
                }
            }
        } catch (SocketException e) {
            // Ignore and return empty
        }
        return "";
    }

    // ==================== Linux Hardware Info ====================

    /**
     * Get Linux BIOS version by reading /sys/class/dmi/id/bios_version
     *
     * @return BIOS version string
     */
    private static String getLinuxBiosVersion() {
        Path biosPath = Paths.get("/sys/class/dmi/id/bios_version");
        try {
            if (Files.exists(biosPath) && Files.isReadable(biosPath)) {
                return Files.readString(biosPath).trim();
            }
        } catch (Exception e) {
            // Fall through to fallback
        }

        // Fallback: try reading /proc/cpuinfo for hardware info
        Path cpuInfoPath = Paths.get("/proc/cpuinfo");
        try {
            if (Files.exists(cpuInfoPath) && Files.isReadable(cpuInfoPath)) {
                String cpuInfo = Files.readString(cpuInfoPath);
                // Extract model name or hardware info
                for (String line : cpuInfo.split("\n")) {
                    if (line.startsWith("model name") || line.startsWith("Hardware")) {
                        return line.split(":")[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Final fallback: use system properties
        return System.getProperty("os.arch", "") + "-" + System.getProperty("os.version", "");
    }

    /**
     * Get Linux system UUID by reading /sys/class/dmi/id/product_uuid
     *
     * @return System UUID string
     */
    private static String getLinuxUUID() {
        Path uuidPath = Paths.get("/sys/class/dmi/id/product_uuid");
        try {
            if (Files.exists(uuidPath) && Files.isReadable(uuidPath)) {
                return Files.readString(uuidPath).trim();
            }
        } catch (Exception e) {
            // Fall through to fallback
        }

        // Fallback: try reading /etc/machine-id
        Path machineIdPath = Paths.get("/etc/machine-id");
        try {
            if (Files.exists(machineIdPath) && Files.isReadable(machineIdPath)) {
                return Files.readString(machineIdPath).trim();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Fallback: try reading /var/lib/dbus/machine-id
        Path dbusIdPath = Paths.get("/var/lib/dbus/machine-id");
        try {
            if (Files.exists(dbusIdPath) && Files.isReadable(dbusIdPath)) {
                return Files.readString(dbusIdPath).trim();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Final fallback: use MAC address
        return getMacAddress();
    }

    // ==================== Windows Hardware Info ====================

    /**
     * Get Windows CPU info using Java native APIs
     * Since we cannot directly access ProcessorId without WMI,
     * we use a combination of system properties and runtime info
     *
     * @return CPU identifier string
     */
    private static String getWindowsCPUInfo() {
        StringBuilder cpuInfo = new StringBuilder();

        // Get OS architecture
        cpuInfo.append(System.getProperty("os.arch", ""));
        cpuInfo.append("-");

        // Get number of processors
        cpuInfo.append(Runtime.getRuntime().availableProcessors());
        cpuInfo.append("-");

        // Get processor identifier from environment (Windows specific)
        String processorId = System.getenv("PROCESSOR_IDENTIFIER");
        if (processorId != null && !processorId.isEmpty()) {
            cpuInfo.append(processorId);
        } else {
            // Fallback to OS name and version
            cpuInfo.append(System.getProperty("os.name", ""));
            cpuInfo.append("-");
            cpuInfo.append(System.getProperty("os.version", ""));
        }

        // Add processor architecture from environment
        String processorArch = System.getenv("PROCESSOR_ARCHITECTURE");
        if (processorArch != null && !processorArch.isEmpty()) {
            cpuInfo.append("-").append(processorArch);
        }

        // Add processor level
        String processorLevel = System.getenv("PROCESSOR_LEVEL");
        if (processorLevel != null && !processorLevel.isEmpty()) {
            cpuInfo.append("-L").append(processorLevel);
        }

        // Add processor revision
        String processorRevision = System.getenv("PROCESSOR_REVISION");
        if (processorRevision != null && !processorRevision.isEmpty()) {
            cpuInfo.append("-R").append(processorRevision);
        }

        return cpuInfo.toString();
    }

    /**
     * Get Windows disk info using Java native APIs
     * Since we cannot directly access disk serial number without WMI,
     * we use file system information as an alternative
     *
     * @return Disk identifier string
     */
    private static String getWindowsDiskInfo() {
        StringBuilder diskInfo = new StringBuilder();

        // Get file system roots information
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                diskInfo.append(root.getAbsolutePath());
                diskInfo.append(":");
                diskInfo.append(root.getTotalSpace());
                diskInfo.append(";");
            }
        }

        // Add computer name from environment
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName != null && !computerName.isEmpty()) {
            diskInfo.append("-").append(computerName);
        }

        // Add user domain
        String userDomain = System.getenv("USERDOMAIN");
        if (userDomain != null && !userDomain.isEmpty()) {
            diskInfo.append("-").append(userDomain);
        }

        // Add user home directory (unique per installation)
        diskInfo.append("-").append(System.getProperty("user.home", ""));

        // If empty, use MAC address as fallback
        if (diskInfo.length() == 0) {
            return getMacAddress();
        }

        return diskInfo.toString();
    }

    // ==================== Mac Hardware Info ====================

    /**
     * Get Mac hardware UUID
     * Try to read from system files or use alternatives
     *
     * @return Hardware UUID string
     */
    private static String getMacHardwareUUID() {
        // Try to read IOPlatformUUID from IORegistry (via file if accessible)
        // Note: Direct access requires root, so we use alternatives

        // Try reading /var/db/.AppleSetupDone file attributes
        Path appleSetupPath = Paths.get("/var/db/.AppleSetupDone");
        try {
            if (Files.exists(appleSetupPath)) {
                BasicFileAttributes attrs = Files.readAttributes(appleSetupPath, BasicFileAttributes.class);
                // Use file creation time as part of identifier
                return "MAC-" + attrs.creationTime().toMillis();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Try reading /Library/Preferences/SystemConfiguration files
        Path[] configPaths = {
                Paths.get("/Library/Preferences/SystemConfiguration/NetworkInterfaces.plist"),
                Paths.get("/Library/Preferences/SystemConfiguration/preferences.plist")
        };

        for (Path configPath : configPaths) {
            try {
                if (Files.exists(configPath)) {
                    BasicFileAttributes attrs = Files.readAttributes(configPath, BasicFileAttributes.class);
                    // Use file info as identifier
                    return "MAC-" + configPath.getFileName() + "-" + attrs.size() + "-" + attrs.creationTime().toMillis();
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Try reading /private/var/db/dslocal/nodes/Default/users/root.plist
        Path rootUserPath = Paths.get("/private/var/db/dslocal/nodes/Default/users/root.plist");
        try {
            if (Files.exists(rootUserPath)) {
                BasicFileAttributes attrs = Files.readAttributes(rootUserPath, BasicFileAttributes.class);
                return "MAC-ROOT-" + attrs.creationTime().toMillis();
            }
        } catch (Exception e) {
            // Ignore
        }

        // Fallback: use MAC address combined with system info
        String macAddress = getMacAddress();
        if (!macAddress.isEmpty()) {
            return "MAC-NET-" + macAddress;
        }

        // Final fallback: use home directory and system properties
        return "MAC-" + System.getProperty("user.home", "") + "-" + System.getProperty("os.version", "");
    }

    /**
     * Get Mac serial number
     * Try to read from accessible system files or use alternatives
     *
     * @return Serial number or alternative identifier
     */
    private static String getMacSerialNumber() {
        // Try to read from various system paths
        Path[] serialPaths = {
                Paths.get("/System/Library/CoreServices/SystemVersion.plist"),
                Paths.get("/System/Library/CoreServices/CoreServices/CoreServices.bundle"),
        };

        for (Path path : serialPaths) {
            try {
                if (Files.exists(path)) {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    return "SERIAL-" + path.getFileName() + "-" + attrs.lastModifiedTime().toMillis();
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        // Try to read SystemVersion.plist content
        Path sysVersionPath = Paths.get("/System/Library/CoreServices/SystemVersion.plist");
        try {
            if (Files.exists(sysVersionPath) && Files.isReadable(sysVersionPath)) {
                String content = Files.readString(sysVersionPath);
                // Extract ProductBuildVersion or ProductVersion
                if (content.contains("ProductBuildVersion")) {
                    int startIdx = content.indexOf("ProductBuildVersion");
                    if (startIdx > 0) {
                        int valueStart = content.indexOf("<string>", startIdx);
                        int valueEnd = content.indexOf("</string>", valueStart);
                        if (valueStart > 0 && valueEnd > valueStart) {
                            String buildVersion = content.substring(valueStart + 8, valueEnd);
                            return "BUILD-" + buildVersion;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Fallback: use system properties
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        return "MACOS-" + osMXBean.getName() + "-" + osMXBean.getVersion() + "-" + osMXBean.getArch();
    }
}
