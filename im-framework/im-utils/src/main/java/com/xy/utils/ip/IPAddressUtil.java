package com.xy.utils.ip;


import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IP / Port 工具类
 * <p>
 * 特性：
 * - 获取本机 IP 地址（IPv4 / IPv6，支持筛选回环 / 虚拟接口 / down 接口）
 * - 检查端口可用性（TCP / UDP / 全面），可指定绑定地址（loopback / all interfaces / specific address）
 * - 查找可用端口（线性或随机），并支持“预占”（reserve）以减少竞态
 * <p>
 * 注意：
 * - 端口可用性检查是瞬时判断（存在竞态）。若需要保证可用性，请使用 reserveTcpPort/reserveUdpPort 立即绑定并保留返回的 Socket。
 * - 对低端端口（<1024）在非管理员环境下可能抛出权限异常（会被视为不可用）。
 */
public final class IPAddressUtil {
    private static final Logger log = Logger.getLogger(IPAddressUtil.class.getName());
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 0xFFFF;

    private IPAddressUtil() {
    }

    // ----------------------------
    // IP 地址相关
    // ----------------------------

    /**
     * 获取本机所有网卡绑定的 IP 地址（可选择 IPv4 / IPv6、是否包含回环/虚拟/未激活接口）
     *
     * @param ipv4Only        true 仅返回 IPv4 地址；false 返回 IPv4 和 IPv6
     * @param includeLoopback 是否包含 loopback 地址（127.0.0.1 / ::1）
     * @param includeVirtual  是否包含 虚拟 网卡
     * @param onlyUp          是否仅包含 isUp() 的网卡
     * @return 不可修改的 InetAddress 列表（可能为空）
     */
    public static List<InetAddress> getAllLocalAddresses(boolean ipv4Only, boolean includeLoopback,
                                                         boolean includeVirtual, boolean onlyUp) {
        List<InetAddress> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) return Collections.emptyList();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                try {
                    if (!includeVirtual && ni.isVirtual()) continue;
                    if (onlyUp) {
                        try {
                            if (!ni.isUp()) continue;
                        } catch (SocketException se) {
                            if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "isUp check failed for " + ni, se);
                            continue;
                        }
                    }
                    Enumeration<InetAddress> addrs = ni.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        if (!includeLoopback && addr.isLoopbackAddress()) continue;
                        if (ipv4Only && !(addr instanceof Inet4Address)) continue;
                        result.add(addr);
                    }
                } catch (Throwable t) {
                    if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "inspect interface failed: " + ni, t);
                }
            }
        } catch (SocketException e) {
            if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, "list network interfaces failed", e);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 返回第一个非回环的 IPv4 地址（优先 site-local: 私有网段），找不到返回空字符串。
     */
    public static String getPrimaryIpv4AddressOrEmpty() {
        InetAddress a = getPrimaryNonLoopbackAddress(true);
        return a == null ? "" : a.getHostAddress();
    }

    /**
     * 返回首选的非回环地址（可选择优先 IPv4），可用于显示或作为服务器监听地址的建议。
     *
     * @param preferIpv4 如果 true 首先尝试返回 IPv4（site-local 优先），否则允许返回 IPv6
     * @return InetAddress 或 null（找不到）
     */
    public static InetAddress getPrimaryNonLoopbackAddress(boolean preferIpv4) {
        try {
            InetAddress candidate = null;
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) return InetAddress.getLocalHost();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress()) continue;
                    // site-local 比较优先（私有网段）
                    if (addr.isSiteLocalAddress()) {
                        if (preferIpv4 && addr instanceof Inet4Address) return addr;
                        if (!preferIpv4 && !(addr instanceof Inet4Address)) return addr;
                        // 否则保存为候选，继续查找更合适的
                        if (candidate == null) candidate = addr;
                    } else {
                        // 非 site-local 也可以作为候选
                        if (candidate == null) candidate = addr;
                    }
                }
            }
            if (candidate != null) return candidate;
            // 最后回退到 InetAddress.getLocalHost()
            try {
                return InetAddress.getLocalHost();
            } catch (Exception e) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "InetAddress.getLocalHost failed", e);
                return null;
            }
        } catch (SocketException | UnknownHostException e) {
            if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, "getPrimaryNonLoopbackAddress failed", e);
            return null;
        }
    }

    /**
     * 更方便的 API：返回首个 site-local IPv4 地址（或空字符串）
     */
    public static String getLocalIp4Address() {
        List<InetAddress> addrs = getAllLocalAddresses(true, false, false, true);
        if (!addrs.isEmpty()) return addrs.get(0).getHostAddress();
        InetAddress primary = getPrimaryNonLoopbackAddress(true);
        return primary == null ? "" : primary.getHostAddress();
    }

    // ----------------------------
    // 端口检测 / 查找 / 预占
    // ----------------------------

    /**
     * 检查端口是否可用（默认在 loopback 地址上 check）。
     * <p>
     * 语义：如果目标地址可在本机上 bind 成功则视为“可用”。默认同时检查 TCP 与 UDP。
     *
     * @param port 要检查的端口
     * @return true 可用，false 不可用或发生异常（异常以 FINE 级别记录）
     */
    public static boolean isPortAvailable(int port) {
        return isPortAvailable(port, InetAddress.getLoopbackAddress(), true, false);
    }

    /**
     * 检查端口是否可用，允许指定：
     *
     * @param port     端口
     * @param bindAddr 要在本机哪个地址上 bind（例如 InetAddress.getLoopbackAddress() 或 0.0.0.0 地址）
     * @param checkTcp 是否检查 TCP（ServerSocket）
     * @param checkUdp 是否检查 UDP（DatagramSocket）
     * @return true 表示在给定 bindAddr 上可用（根据 checkTcp/checkUdp 的组合）
     */
    public static boolean isPortAvailable(int port, InetAddress bindAddr, boolean checkTcp, boolean checkUdp) {
        validatePort(port);
        Objects.requireNonNull(bindAddr, "bindAddr");
        if (!checkTcp && !checkUdp) throw new IllegalArgumentException("either checkTcp or checkUdp must be true");

        if (checkTcp) {
            if (!isTcpPortAvailable(port, bindAddr)) return false;
        }
        if (checkUdp) {
            if (!isUdpPortAvailable(port, bindAddr)) return false;
        }
        return true;
    }

    private static boolean isTcpPortAvailable(int port, InetAddress bindAddr) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(bindAddr, port));
            return true;
        } catch (IOException | SecurityException e) {
            if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "TCP port bind failed: " + port + " on " + bindAddr, e);
            return false;
        }
    }

    private static boolean isUdpPortAvailable(int port, InetAddress bindAddr) {
        try (DatagramSocket ds = new DatagramSocket(null)) {
            ds.setReuseAddress(true);
            ds.bind(new InetSocketAddress(bindAddr, port));
            return true;
        } catch (SocketException | SecurityException e) {
            if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "UDP port bind failed: " + port + " on " + bindAddr, e);
            return false;
        }
    }

    // -------------- convenience finders --------------

    /**
     * 在给定范围内查找第一个可用端口（包含 from，不超过 to），默认在 loopback 上检查 TCP+UDP。
     * <p>找到后返回端口号；若未找到返回 -1。</p>
     */
    public static int findFirstAvailablePort(int fromInclusive, int toInclusive) {
        return findFirstAvailablePort(fromInclusive, toInclusive, InetAddress.getLoopbackAddress(), true, true);
    }

    /**
     * 在区间内查找第一个可用端口（可指定 bind 地址与 TCP/UDP 检查项）。
     */
    public static int findFirstAvailablePort(int fromInclusive, int toInclusive, InetAddress bindAddr,
                                             boolean checkTcp, boolean checkUdp) {
        if (fromInclusive <= 0 || toInclusive > MAX_PORT || fromInclusive > toInclusive) {
            throw new IllegalArgumentException("invalid port range");
        }
        for (int p = fromInclusive; p <= toInclusive; p++) {
            if (isPortAvailable(p, bindAddr, checkTcp, checkUdp)) return p;
        }
        return -1;
    }

    /**
     * 在区间内随机查找一个可用端口（非均匀，但可减少连续扫描时的竞态）。
     * 返回 -1 表示未找到。
     */
    public static int findRandomAvailablePortInRange(int fromInclusive, int toInclusive) {
        if (fromInclusive <= 0 || toInclusive > MAX_PORT || fromInclusive > toInclusive) {
            throw new IllegalArgumentException("invalid port range");
        }
        int size = toInclusive - fromInclusive + 1;
        List<Integer> seq = new ArrayList<>(size);
        for (int p = fromInclusive; p <= toInclusive; p++) seq.add(p);
        Collections.shuffle(seq);
        for (int p : seq) {
            if (isPortAvailable(p)) return p;
        }
        return -1;
    }

    /**
     * 请求系统分配一个空闲端口（绑定端口 0），并返回该端口号（关闭 socket 后端口可能随即被占用）。
     * 常用于获取“一个临时端口号”的场景（注意竞态）。
     */
    public static int getFreePortFromSystem() {
        try (ServerSocket ss = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return ss.getLocalPort();
        } catch (IOException e) {
            if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "getFreePortFromSystem failed", e);
            return -1;
        }
    }

    // ----------------------------
    // 预占（reserve）方法：找到可用端口并立即绑定返回 Socket（调用方负责 close）
    // ----------------------------

    /**
     * 在给定端口范围内查找并立即绑定 TCP ServerSocket（用于避免竞态）。
     * 返回已绑定的 ServerSocket（调用者必须在不需要时 close()）。
     * 找不到返回 null。
     */
    public static ServerSocket reserveTcpPort(int fromInclusive, int toInclusive) {
        if (fromInclusive <= 0 || toInclusive > MAX_PORT || fromInclusive > toInclusive) {
            throw new IllegalArgumentException("invalid port range");
        }
        for (int p = fromInclusive; p <= toInclusive; p++) {
            try {
                ServerSocket ss = new ServerSocket();
                ss.setReuseAddress(true);
                ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), p));
                // 成功绑定，返回 socket（caller 负责 close）
                return ss;
            } catch (IOException | SecurityException e) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "reserveTcpPort failed for " + p, e);
                // try next
            }
        }
        return null;
    }

    /**
     * 在给定端口范围内查找并立即绑定 UDP DatagramSocket（调用方负责 close）。
     */
    public static DatagramSocket reserveUdpPort(int fromInclusive, int toInclusive) {
        if (fromInclusive <= 0 || toInclusive > MAX_PORT || fromInclusive > toInclusive) {
            throw new IllegalArgumentException("invalid port range");
        }
        for (int p = fromInclusive; p <= toInclusive; p++) {
            try {
                DatagramSocket ds = new DatagramSocket(null);
                ds.setReuseAddress(true);
                ds.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), p));
                return ds;
            } catch (IOException | SecurityException e) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "reserveUdpPort failed for " + p, e);
            }
        }
        return null;
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private static void validatePort(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
    }

    // ----------------------------
    // 简单使用示例（可用于测试）
    // ----------------------------
//    public static void main(String[] args) throws IOException {
//        System.out.println("Local IPv4 (primary): " + getLocalIp4Address());
//
//        int start = 8000, end = 8100;
//        int found = findFirstAvailablePort(start, end);
//        System.out.println("first avail " + start + ".." + end + " -> " + found);
//
//        int free = getFreePortFromSystem();
//        System.out.println("system free port: " + free);
//
//        // demo reserve
//        try (ServerSocket reserved = reserveTcpPort(9000, 9010)) {
//            if (reserved != null) {
//                System.out.println("reserved tcp port " + reserved.getLocalPort());
//            } else {
//                System.out.println("no tcp port reserved");
//            }
//        }
//    }
}
