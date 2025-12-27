package com.xy.lucky.utils.id;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * IdUtils - 统一的 ID/UUID 生成工具类
 * <p>
 * 特性：
 * - 标准 UUID / 简化 UUID（去掉 -）
 * - Base62 编码的 UUID（更短，URL 友好）
 * - ULID（可排序的 26 字符 Crockford-Base32）
 * - 可配置的 Snowflake（雪花）ID 生成器（支持自定义 epoch/datacenter/machine）
 * - ShortId（将 UUID 或雪花进行 Base62 编码，生成更短字符串）
 * <p>
 * <p>
 * 示例：
 * IdUtils.randomUUID()
 * IdUtils.simpleUUID()
 * IdUtils.base62Uuid()
 * IdUtils.ulid()
 * IdUtils.snowflakeId()
 * IdUtils.shortIdFromUuid(UUID.randomUUID())
 * <p>
 * 初始化自定义 Snowflake：
 * IdUtils.initSnowflake(2, 3, 1600000000000L, true);
 */
@Getter
public final class IdUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ---------------------- 随机与 UUID 相关 ----------------------
    private static final char[] CROCKFORD_BASE32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    // 默认单例 Snowflake（datacenter=1, machine=1），可通过 initSnowflake 改变
    private static volatile SnowflakeIdGenerator DEFAULT_SNOWFLAKE = new SnowflakeIdGenerator(1, 1);

    private IdUtils() { /* no-op */ }

    /**
     * 生成标准 UUID（带短横线）
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成简化 UUID（去掉横线, 小写）
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // ---------------------- ULID（可排序）实现 ----------------------

    /**
     * 生成大写简化 UUID（便于日志或键比较时使用）
     */
    public static String simpleUUIDUpper() {
        return simpleUUID().toUpperCase();
    }


    /**
     * 把 UUID（128-bit）转换为 Base62（紧凑、URL 友好）字符串。
     * 输出长度通常为 22 或 23 个字符（取决于实现细节）
     */
    public static String base62Uuid() {
        UUID uuid = UUID.randomUUID();
        return base62FromBytes(uuidToBytes(uuid));
    }

    /**
     * 生成指定长度的随机 Base62 字符串（字符均在 0-9A-Za-z）
     * 注意：该方法是随机字符串，不保证分布式唯一性，仅适合作为短 token
     *
     * @param length 期望长度
     */
    public static String randomBase62(int length) {
        if (length <= 0) throw new IllegalArgumentException("length must be > 0");
        byte[] buf = new byte[length];
        SECURE_RANDOM.nextBytes(buf);
        return base62FromBytes(buf).substring(0, length);
    }

    // ---------------------- Snowflake（雪花）实现 ----------------------

    /**
     * 生成 ULID（26 字符 Crockford Base32，可按时间排序）
     * 实现要点：
     * - 48 位时间戳（毫秒）
     * - 80 位随机数
     * - 最终编码 26 个字符
     */
    public static String ulid() {
        long time = Instant.now().toEpochMilli();
        // 48-bit timestamp
        byte[] bytes = new byte[16]; // 128-bit buffer, will use 6 bytes timestamp + 10 bytes randomness
        // timestamp (6 bytes big-endian)
        bytes[0] = (byte) ((time >>> 40) & 0xFF);
        bytes[1] = (byte) ((time >>> 32) & 0xFF);
        bytes[2] = (byte) ((time >>> 24) & 0xFF);
        bytes[3] = (byte) ((time >>> 16) & 0xFF);
        bytes[4] = (byte) ((time >>> 8) & 0xFF);
        bytes[5] = (byte) ((time) & 0xFF);
        // randomness (10 bytes)
        byte[] rand = new byte[10];
        SECURE_RANDOM.nextBytes(rand);
        System.arraycopy(rand, 0, bytes, 6, 10);
        return encodeCrockfordBase32(bytes);
    }

    // encode 128-bit buffer as 26-char Crockford base32 (ULID)
    private static String encodeCrockfordBase32(byte[] bytes) {
        // ULID encodes 128 bits into 26 base32 chars (26*5 = 130 bits, leading 2 bits are zero)
        // We'll treat as big-endian bits.
        int index = 0;
        int bitBuffer = 0;
        int bitBufferLength = 0;
        StringBuilder sb = new StringBuilder(26);
        for (byte b : bytes) {
            bitBuffer = (bitBuffer << 8) | (b & 0xFF);
            bitBufferLength += 8;
            while (bitBufferLength >= 5) {
                int shift = bitBufferLength - 5;
                int val = (bitBuffer >>> shift) & 0x1F;
                sb.append(CROCKFORD_BASE32[val]);
                bitBufferLength -= 5;
                bitBuffer &= (1 << shift) - 1;
                index++;
                if (index == 26) return sb.toString();
            }
        }
        // padding (if any bits remain, pad with zeros to 5 bits and encode)
        if (index < 26 && bitBufferLength > 0) {
            int val = (bitBuffer << (5 - bitBufferLength)) & 0x1F;
            sb.append(CROCKFORD_BASE32[val]);
            index++;
        }
        // If still shorter, pad with '0'
        while (index < 26) {
            sb.append('0');
            index++;
        }
        return sb.toString();
    }

    /**
     * 使用默认配置生成雪花 long id
     */
    public static long snowflakeId() {
        return DEFAULT_SNOWFLAKE.nextId();
    }

    /**
     * 使用默认配置生成雪花 id（字符串）
     */
    public static String snowflakeIdStr() {
        return String.valueOf(snowflakeId());
    }

    /**
     * 获取 Snowflake 实例（可作更细粒度控制）
     */
    public static SnowflakeIdGenerator getSnowflake() {
        return DEFAULT_SNOWFLAKE;
    }

    /**
     * 用自定义参数初始化默认 Snowflake（线程安全）
     *
     * @param datacenterId        数据中心ID
     * @param machineId           机器ID
     * @param epoch               自定义起始时间（毫秒），如果为 null 则使用实现默认
     * @param waitOnClockBackward 是否在时钟回拨时等待直到时间追上；false 则抛异常
     */
    public static synchronized void initSnowflake(long datacenterId, long machineId, Long epoch, boolean waitOnClockBackward) {
        DEFAULT_SNOWFLAKE = new SnowflakeIdGenerator(datacenterId, machineId, epoch, waitOnClockBackward);
    }

    // ---------------------- ShortId 工具 ----------------------

    /**
     * 将 long 型雪花 ID 编码为 Base62（短字符串）
     */
    public static String shortIdFromSnowflake(long id) {
        return base62FromBytes(longToBytes(id));
    }

    // ---------------------- 编码器：Base62 ----------------------

    /**
     * 将任意 UUID 字符串压缩为 Base62 短 ID（强烈推荐用于需要短且可 URL 的标识）
     */
    public static String shortIdFromUuid(UUID uuid) {
        return base62FromBytes(uuidToBytes(uuid));
    }

    /**
     * 把字节数组编码为 Base62 字符串（不是最紧凑的数字空间编码，但实现简单）
     * 说明：实现将 bytes 当作一个无符号大整数来做除以 62 的变换。
     */
    public static String base62FromBytes(byte[] data) {
        if (data == null || data.length == 0) return "";
        // Convert to positive BigInteger-like division by 62 without using BigInteger to avoid extra allocation complexity
        // We'll use java.math.BigInteger for simplicity and clarity.
        java.math.BigInteger value = new java.math.BigInteger(1, data);
        StringBuilder sb = new StringBuilder();
        java.math.BigInteger base = java.math.BigInteger.valueOf(62);
        while (value.compareTo(java.math.BigInteger.ZERO) > 0) {
            java.math.BigInteger[] divmod = value.divideAndRemainder(base);
            value = divmod[0];
            int idx = divmod[1].intValue();
            sb.append(BASE62[idx]);
        }
        // Big-endian string
        return sb.length() == 0 ? "0" : sb.reverse().toString();
    }

    // ---------------------- 辅助转换 ----------------------

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static byte[] longToBytes(long v) {
        ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
        bb.putLong(v);
        return bb.array();
    }

    // ---------------------- 内部 SnowflakeIdGenerator 类 ----------------------

    /**
     * 高可配置的 Snowflake 实现（线程安全）
     * - 支持自定义 epoch（起始时间）
     * - 支持对时钟回拨的两种策略：等待或抛异常
     */
    public static class SnowflakeIdGenerator {
        // 默认起始时间：2021-01-01
        private static final long DEFAULT_EPOCH = 1609459200000L;

        private final long epoch;
        private final long datacenterId;
        private final long machineId;
        private final boolean waitOnClockBackward;

        private final int sequenceBits = 12;
        private final int machineBits = 5;
        private final int datacenterBits = 5;

        private final long maxDatacenterId = ~(-1L << datacenterBits);
        private final long maxMachineId = ~(-1L << machineBits);
        private final long maxSequence = ~(-1L << sequenceBits);

        private final long machineShift = sequenceBits;
        private final long datacenterShift = sequenceBits + machineBits;
        private final long timestampLeftShift = datacenterShift + datacenterBits;

        private long sequence = 0L;
        private long lastTimestamp = -1L;

        /**
         * 使用默认 epoch
         */
        public SnowflakeIdGenerator(long datacenterId, long machineId) {
            this(datacenterId, machineId, DEFAULT_EPOCH, true);
        }

        /**
         * 完整构造器
         *
         * @param datacenterId        数据中心ID
         * @param machineId           机器ID
         * @param epoch               起始时间（ms），若 null 则使用默认
         * @param waitOnClockBackward 时钟回拨时是否等待（true）或抛异常（false）
         */
        public SnowflakeIdGenerator(long datacenterId, long machineId, Long epoch, boolean waitOnClockBackward) {
            if (datacenterId < 0 || datacenterId > maxDatacenterId)
                throw new IllegalArgumentException("datacenterId out of range");
            if (machineId < 0 || machineId > maxMachineId)
                throw new IllegalArgumentException("machineId out of range");
            this.datacenterId = datacenterId;
            this.machineId = machineId;
            this.epoch = epoch == null ? DEFAULT_EPOCH : epoch;
            this.waitOnClockBackward = waitOnClockBackward;
        }

        /**
         * 生成下一个 ID（线程安全）
         */
        public synchronized long nextId() {
            long timestamp = currentTime();
            if (timestamp < lastTimestamp) {
                long diff = lastTimestamp - timestamp;
                if (!waitOnClockBackward) {
                    throw new RuntimeException("Clock moved backwards by " + diff + " ms, refusing to generate id");
                }
                // 等待直到时间追上
                long until = waitUntil(lastTimestamp);
                timestamp = until;
            }

            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & maxSequence;
                if (sequence == 0) {
                    // 序列耗尽，等待下一毫秒
                    timestamp = waitUntil(lastTimestamp + 1);
                }
            } else {
                sequence = 0L;
            }
            lastTimestamp = timestamp;

            return ((timestamp - epoch) << timestampLeftShift)
                    | (datacenterId << datacenterShift)
                    | (machineId << machineShift)
                    | sequence;
        }

        private long currentTime() {
            return System.currentTimeMillis();
        }

        private long waitUntil(long ts) {
            long t;
            do {
                t = currentTime();
            } while (t < ts);
            return t;
        }
    }


}
