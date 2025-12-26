package com.xy.lucky.logging.service;

import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.mapper.LogRecordConverter;
import com.xy.lucky.logging.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 日志查询服务
 * 负责日志的检索与清理逻辑，确保查询高效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogQueryService {
    private final LogRepository repository;
    private final LogRecordConverter converter;
    private final DiscoveryClient discoveryClient;

    /**
     * 查询日志
     *
     * @param module  模块名
     * @param start   开始时间
     * @param end     结束时间
     * @param level   日志级别
     * @param page    页码
     * @param size    每页大小
     * @param keyword 关键字
     * @return 日志列表
     */
    public List<LogRecordVo> query(String module, Instant start, Instant end, LogLevel level, String service, String env, int page, int size, String keyword) {
        log.info("执行日志查询: module={} service={} env={} level={} keyword={}", module, service, env, level, keyword);
        Instant actualStart = start != null ? start : Instant.EPOCH;
        Instant actualEnd = end != null ? end : Instant.now();
        int actualPage = Math.max(page, 0);
        int actualSize = Math.max(size, 1);
        int offset = actualPage * actualSize;
        try {
            List<LogPo> list = repository.queryRange(
                    module,
                    actualStart,
                    actualEnd,
                    level != null ? level.name() : null,
                    service,
                    env,
                    keyword,
                    offset,
                    actualSize
            );
            return list.stream().map(converter::toVo).toList();
        } catch (Exception ex) {
            log.warn("查询日志失败，返回空列表: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<LogRecordVo> search(String module, Instant start, Instant end, List<LogLevel> levels, int from, int size, String keyword) {
        LogLevel level = levels != null && !levels.isEmpty() ? levels.get(0) : null;
        int page = from > 0 ? from / Math.max(size, 1) : 0;
        return query(module, start, end, level, null, null, page, size, keyword);
    }

    public Map<String, Long> histogram(String module, Instant start, Instant end, LogLevel level, String service, String env, String keyword, String interval) {
        Instant actualStart = start != null ? start : Instant.EPOCH;
        Instant actualEnd = end != null ? end : Instant.now();
        // Postgres format: 'YYYY-MM-DD HH24' for hour, 'YYYY-MM-DD HH24:MI' for minute
        String format = "YYYY-MM-DD HH24";
        if ("minute".equalsIgnoreCase(interval)) {
            format = "YYYY-MM-DD HH24:MI";
        }
        try {
            List<Object[]> list = repository.queryHistogram(
                    module, actualStart, actualEnd,
                    level != null ? level.name() : null,
                    service, env, keyword, format
            );
            java.util.LinkedHashMap<String, Long> result = new java.util.LinkedHashMap<>();
            for (Object[] row : list) {
                result.put((String) row[0], ((Number) row[1]).longValue());
            }
            return result;
        } catch (Exception ex) {
            log.warn("Histogram query failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    public List<String> listServices(String env) {
        try {
            return discoveryClient.getServices();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<String> listModules() {
        try {
            return repository.listModules();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<String> listAddresses(String service) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            return instances.stream().map(ServiceInstance::getHost).toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<Map<String, Object>> topServices(Instant start, Instant end, int limit) {
        try {
            List<Object[]> rows = repository.topServices(start, end, limit);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                result.add(Map.of(
                        "name", String.valueOf(r[0]),
                        "count", ((Number) r[1]).longValue()
                ));
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<Map<String, Object>> topAddresses(Instant start, Instant end, int limit) {
        try {
            List<Object[]> rows = repository.topAddresses(start, end, limit);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                result.add(Map.of(
                        "name", String.valueOf(r[0]),
                        "count", ((Number) r[1]).longValue()
                ));
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    public List<Map<String, Object>> topErrorTypes(Instant start, Instant end, int limit) {
        try {
            List<Object[]> rows = repository.topErrorTypes(start, end, limit);
            List<Map<String, Object>> result = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                result.add(Map.of(
                        "name", String.valueOf(r[0]),
                        "count", ((Number) r[1]).longValue()
                ));
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }
    /**
     * 删除指定时间之前的日志
     *
     * @param cutoff 截止时间
     */
    public void deleteBefore(Instant cutoff) {
        log.warn("删除日志: cutoff={}", cutoff);
        try {
            repository.deleteByTsBefore(cutoff);
        } catch (Exception ex) {
            log.warn("删除日志失败: {}", ex.getMessage());
        }
    }

    /**
     * 删除指定模块在指定时间之前的日志
     *
     * @param module 模块名
     * @param cutoff 截止时间
     */
    public void deleteModuleBefore(String module, Instant cutoff) {
        log.warn("删除模块日志: module={}, cutoff={}", module, cutoff);
        try {
            repository.deleteByModuleAndTsBefore(module, cutoff);
        } catch (Exception ex) {
            log.warn("删除模块日志失败: {}", ex.getMessage());
        }
    }
}
