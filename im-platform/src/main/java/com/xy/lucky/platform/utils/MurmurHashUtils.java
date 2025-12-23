package com.xy.lucky.platform.utils;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * MurmurHashUtils
 * <p>
 * 功能：
 * - 提供 Murmur3 的哈希并输出 Base62 编码字符串
 * - 支持 32-bit（短）和 128-bit（更长、更低碰撞）两种模式
 * - 支持用户隔离（同一 URL，不同 userId 生成不同短码）
 * <p>
 * 注意：
 * - Murmur 系列是非加密哈希（非密码学安全），适合短链接、分片路由、快速散列等场景，但不用于安全校验或签名。
 * - Base62 编码区分大小写（0-9,A-Z,a-z），紧凑且 URL 友好。
 */
public final class MurmurHashUtils {

    // Base62 字符集（0-9, A-Z, a-z）
    private static final char[] BASE62 = buildBase62Chars();
    private static final int RADIX = BASE62.length;

    private MurmurHashUtils() { /* no-op */ }

    private static char[] buildBase62Chars() {
        char[] cs = new char[62];
        int i = 0;
        for (char c = '0'; c <= '9'; c++) cs[i++] = c;
        for (char c = 'A'; c <= 'Z'; c++) cs[i++] = c;
        for (char c = 'a'; c <= 'z'; c++) cs[i++] = c;
        return cs;
    }

    // ========================= Public API =========================

    /**
     * 使用 Murmur3 32-bit（固定种子）生成 Base62 字符串（很短，适合非常短的标识）
     *
     * @param input 非空字符串
     * @return 非空 Base62 编码字符串（至少 "0"）
     */
    public static String create32(String input) {
        Objects.requireNonNull(input, "input cannot be null");
        HashCode hc = Hashing.murmur3_32_fixed().hashString(input, StandardCharsets.UTF_8);
        int signed = hc.asInt();
        long unsigned = signed & 0xFFFFFFFFL; // 转为无符号 long
        return base62FromLong(unsigned);
    }

    /**
     * 使用 Murmur3 128-bit 生成 Base62 字符串（更长、碰撞更少，推荐用于生产）
     *
     * @param input 非空字符串
     * @return Base62 编码字符串（长度根据内容，通常比 32-bit 长）
     */
    public static String create128(String input) {
        Objects.requireNonNull(input, "input cannot be null");
        HashCode hc = Hashing.murmur3_128().hashString(input, StandardCharsets.UTF_8);
        byte[] bytes = hc.asBytes();
        return base62FromBytes(bytes);
    }

    /**
     * 默认生成器：使用 128-bit Murmur（更安全）
     *
     * @param input 非空字符串
     * @return Base62 编码字符串
     */
    public static String createAuto(String input) {
        return create128(input);
    }

    /**
     * 支持用户隔离：将 url 与 userId 拼接后再哈希，保证相同 url 不同 userId 产生不同结果
     *
     * @param url    非空原始 url
     * @return Base62 字符串
     */
    public static String createWithUser(String url) {
        Objects.requireNonNull(url, "url cannot be null");
        return createAuto(url);
    }

    /**
     * 更短但风险更高的变体：对 128-bit 的输出进行截断并返回指定长度（长度 <= full length）
     * 用法：当你需要固定长度（如 8/10/12）并能接受更高碰撞风险时使用
     *
     * @param input 原始字符串
     * @param len   期望长度（>0）
     * @return 固定长度的 Base62 字符串（可能发生截断）
     */
    public static String create128Truncated(String input, int len) {
        String full = create128(input);
        if (len <= 0) throw new IllegalArgumentException("len must > 0");
        if (full.length() <= len) return full;
        return full.substring(0, len);
    }

    // ========================= 编码实现 =========================

    /**
     * 将 long（非负）编码为 Base62
     *
     * @param value 非负 long（通常由 unsigned 32-bit 或 64-bit）
     * @return Base62 字符串（"0" 表示 0）
     */
    private static String base62FromLong(long value) {
        if (value == 0L) return "0";
        StringBuilder sb = new StringBuilder();
        long v = value;
        while (v > 0) {
            int idx = (int) (v % RADIX);
            sb.append(BASE62[idx]);
            v /= RADIX;
        }
        return sb.reverse().toString();
    }

    /**
     * 将任意字节数组（通常为哈希输出）编码为 Base62（使用 BigInteger 做大整数转换）
     *
     * @param bytes 非空字节数组
     * @return Base62 字符串（至少 "0"）
     */
    private static String base62FromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "0";
        // 使用 BigInteger(1, bytes) 将 bytes 当作无符号整数
        BigInteger bi = new BigInteger(1, bytes);
        if (bi.equals(BigInteger.ZERO)) return "0";
        StringBuilder sb = new StringBuilder();
        BigInteger base = BigInteger.valueOf(RADIX);
        while (bi.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] dr = bi.divideAndRemainder(base);
            bi = dr[0];
            int idx = dr[1].intValue();
            sb.append(BASE62[idx]);
        }
        return sb.reverse().toString();
    }

    // ========================= 兼容/便捷方法 =========================

    /**
     * 将输入做 trim 后计算（避免前后空格导致不可预测结果）
     */
    public static String create32Safe(String input) {
        return create32(normalize(input));
    }

    public static String create128Safe(String input) {
        return create128(normalize(input));
    }

    public static String createAutoSafe(String input) {
        return createAuto(normalize(input));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }

    // ========================= 使用示例（注释） =========================
    /*
        String short1 = MurmurHashUtils.create32("https://example.com/long/url");
        String strong = MurmurHashUtils.create128("https://example.com/long/url");
        String perUser = MurmurHashUtils.createWithUser("https://example.com/long/url", "user123");
        String fixed8 = MurmurHashUtils.create128Truncated("https://example.com/long/url", 8);
     */

}

