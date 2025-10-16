package com.xy.utils;

import java.util.UUID;

/**
 * ID生成器工具类
 */
public class IdUtils {
    /**
     * 获取随机UUID
     *
     * @return 随机UUID
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 雪花ID生成器
     */
    private static SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
    
    /**
     * 生成雪花ID
     * 
     * @return 雪花ID
     */
    public static long snowflakeId() {
        return snowflakeIdGenerator.nextId();
    }
    
    /**
     * 生成雪花ID（字符串形式）
     * 
     * @return 雪花ID字符串
     */
    public static String snowflakeIdStr() {
        return String.valueOf(snowflakeId());
    }

    /**
     * 雪花ID生成器内部类
     */
    private static class SnowflakeIdGenerator {
        // 起始时间戳 (2021-01-01)
        private final static long START_TIMESTAMP = 1609459200000L;
        
        // 各部分位数
        private final static long SEQUENCE_BIT = 12;
        private final static long MACHINE_BIT = 5;
        private final static long DATACENTER_BIT = 5;
        
        // 最大值
        private final static long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
        private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
        private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);
        
        // 各部分偏移量
        private final static long MACHINE_LEFT = SEQUENCE_BIT;
        private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
        private final static long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;
        
        // 数据中心ID和机器ID
        private long datacenterId;
        private long machineId;
        private long sequence = 0L;
        private long lastTimestamp = -1L;
        
        /**
         * 构造函数
         * 
         * @param datacenterId 数据中心ID
         * @param machineId 机器ID
         */
        public SnowflakeIdGenerator(long datacenterId, long machineId) {
            if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
                throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
            }
            if (machineId > MAX_MACHINE_NUM || machineId < 0) {
                throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
            }
            this.datacenterId = datacenterId;
            this.machineId = machineId;
        }
        
        /**
         * 生成下一个ID
         * 
         * @return 下一个ID
         */
        public synchronized long nextId() {
            long currTimestamp = getNewTimestamp();
            if (currTimestamp < lastTimestamp) {
                throw new RuntimeException("Clock moved backwards. Refusing to generate id");
            }
            
            if (currTimestamp == lastTimestamp) {
                //相同毫秒内，序列号自增
                sequence = (sequence + 1) & MAX_SEQUENCE;
                //同一毫秒的序列数已经达到最大
                if (sequence == 0L) {
                    currTimestamp = getNextMill();
                }
            } else {
                //不同毫秒内，序列号置为0
                sequence = 0L;
            }
            
            lastTimestamp = currTimestamp;
            
            return (currTimestamp - START_TIMESTAMP) << TIMESTAMP_LEFT
                    | datacenterId << DATACENTER_LEFT
                    | machineId << MACHINE_LEFT
                    | sequence;
        }
        
        private long getNextMill() {
            long mill = getNewTimestamp();
            while (mill <= lastTimestamp) {
                mill = getNewTimestamp();
            }
            return mill;
        }
        
        private long getNewTimestamp() {
            return System.currentTimeMillis();
        }
    }
}