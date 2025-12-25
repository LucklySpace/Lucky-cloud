package com.xy.lucky.logging.service;

import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志分析服务
 * 负责日志的聚合统计，使用Redis存储统计数据，确保高性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisService {
    private final RedisTemplate<String, Object> redisTemplate;

    private String keyLevelCount(String level) {
        return "logs:stat:level:" + level;
    }

    private String keyModuleCount(String module) {
        return "logs:stat:module:" + module;
    }

    private String keyBucketHour(String hour, String level) {
        return "logs:stat:hour:" + hour + ":" + level;
    }

    /**
     * 聚合日志统计
     * 更新Redis中的级别、模块和小时桶计数
     *
     * @param record 日志记录
     */
    public void aggregate(LogRecordVo record) {
        if (record.getLevel() != null) {
            redisTemplate.opsForValue().increment(keyLevelCount(record.getLevel().name()));
        }
        if (record.getModule() != null) {
            redisTemplate.opsForValue().increment(keyModuleCount(record.getModule()));
        }
        if (record.getTimestamp() != null && record.getLevel() != null) {
            String hour = DateTimeFormatter.ofPattern("yyyyMMddHH")
                    .withZone(ZoneId.systemDefault())
                    .format(record.getTimestamp());
            redisTemplate.opsForValue().increment(keyBucketHour(hour, record.getLevel().name()));
        }
        if (log.isDebugEnabled()) {
            log.debug("聚合日志统计: id={}, level={}", record.getId(), record.getLevel());
        }
    }

    /**
     * 获取日志统计概览
     *
     * @return 统计数据地图
     */
    public Map<String, Object> overview() {
        log.info("生成日志概览统计");
        Map<String, Object> result = new HashMap<>();
        // Collect common levels
        String[] levels = new String[]{"TRACE", "DEBUG", "INFO", "WARN", "ERROR"};
        Map<String, Long> levelCounts = new HashMap<>();
        for (String level : levels) {
            Object val = redisTemplate.opsForValue().get(keyLevelCount(level));
            levelCounts.put(level, val instanceof Number ? ((Number) val).longValue() : 0L);
        }
        result.put("levels", levelCounts);
        result.put("modules", new HashMap<>()); // Modules can be enumerated externally
        return result;
    }

    /**
     * 获取小时级日志序列
     *
     * @param level 日志级别
     * @param hours 最近小时数
     * @return 时间序列数据
     */
    public Map<String, Long> hourlySeries(String level, int hours) {
        log.info("生成小时级日志序列: level={}, hours={}", level, hours);
        Map<String, Long> series = new HashMap<>();
        for (int i = hours - 1; i >= 0; i--) {
            String hour = java.time.ZonedDateTime.now().minusHours(i).format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
            String key = keyBucketHour(hour, level);
            Object val = redisTemplate.opsForValue().get(key);
            series.put(hour, val instanceof Number ? ((Number) val).longValue() : 0L);
        }
        return series;
    }
}