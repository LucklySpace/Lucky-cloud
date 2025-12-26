package com.xy.lucky.logging.service;

import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.exception.LoggingException;
import com.xy.lucky.logging.mapper.LogMapper;
import com.xy.lucky.logging.repository.LogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final LogMapper logMapper;
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
    public Page<LogRecordVo> query(String module, LocalDateTime start, LocalDateTime end, LogLevel level, String service, String env, int page, int size, String keyword) {
        log.info("执行日志查询: module={} service={} env={} level={} keyword={}", module, service, env, level, keyword);
        LocalDateTime actualStart = start != null ? start : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime actualEnd = end != null ? end : LocalDateTime.now();
        int actualPage = Math.max(page, 1);
        int actualSize = Math.max(size, 10);
        String m = normalize(module);
        String svc = normalize(service);
        String environment = normalize(env);
        String kw = normalize(keyword);
        try {
            LogPo probe = new LogPo();
            if (m != null) probe.setModule(m);
            if (level != null) probe.setLevel(level.name());
            if (svc != null) probe.setService(svc);
            if (environment != null) probe.setEnv(environment);
            if (kw != null) probe.setMessage(kw);
            ExampleMatcher matcher = ExampleMatcher.matchingAll()
                    .withIgnoreCase()
                    .withStringMatcher(ExampleMatcher.StringMatcher.EXACT)
                    .withMatcher("message", match -> match.contains().ignoreCase());
            Example<LogPo> example = Example.of(probe, matcher);
            Page<LogPo> pageData = repository.findAll(getSpecFromDatesAndExample(actualStart, actualEnd, example), PageRequest.of(actualPage, actualSize, Sort.by(Sort.Direction.DESC, "ts")));
            List<LogRecordVo> list = pageData.getContent().stream().map(logMapper::toVo).toList();
            return new PageImpl<>(list, pageData.getPageable(), pageData.getTotalElements());
        } catch (Exception ex) {
            log.warn("查询日志失败，返回空列表: {}", ex.getMessage());
            throw new LoggingException("查询日志失败");
        }
    }

    public Specification<LogPo> getSpecFromDatesAndExample(
            LocalDateTime from, LocalDateTime to, Example<LogPo> example) {

        return (root, query, builder) -> {
            final List<Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(builder.greaterThan(root.get("ts"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("ts"), to));
            }
            predicates.add(QueryByExamplePredicateBuilder.getPredicate(root, builder, example));

            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

    ;

    public Map<String, Long> histogram(String module, LocalDateTime start, LocalDateTime end, LogLevel level, String service, String env, String keyword, String interval) {
        LocalDateTime actualStart = start != null ? start : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime actualEnd = end != null ? end : LocalDateTime.now();
        String format = "YYYY-MM-DD HH24";
        if ("minute".equalsIgnoreCase(interval)) {
            format = "YYYY-MM-DD HH24:MI";
        }
        String m = normalize(module);
        String svc = normalize(service);
        String environment = normalize(env);
        String kw = normalize(keyword);
        try {
            List<Object[]> list = repository.queryHistogram(
                    m, actualStart, actualEnd,
                    level != null ? level.name() : null,
                    svc, environment, kw, format
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

    public List<Map<String, Object>> topServices(LocalDateTime start, LocalDateTime end, int limit) {
        try {
            LocalDateTime actualStart = start != null ? start : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime actualEnd = end != null ? end : LocalDateTime.now();
            List<Object[]> rows = repository.topServices(actualStart, actualEnd, limit);
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

    public List<Map<String, Object>> topAddresses(LocalDateTime start, LocalDateTime end, int limit) {
        try {
            LocalDateTime actualStart = start != null ? start : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime actualEnd = end != null ? end : LocalDateTime.now();
            List<Object[]> rows = repository.topAddresses(actualStart, actualEnd, limit);
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

    public List<Map<String, Object>> topErrorTypes(LocalDateTime start, LocalDateTime end, int limit) {
        try {
            LocalDateTime actualStart = start != null ? start : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime actualEnd = end != null ? end : LocalDateTime.now();
            List<Object[]> rows = repository.topErrorTypes(actualStart, actualEnd, limit);
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

    public void deleteBefore(LocalDateTime cutoff) {
        log.warn("删除日志: cutoff={}", cutoff);
        try {
            repository.deleteByTsBefore(cutoff);
        } catch (Exception ex) {
            log.warn("删除日志失败: {}", ex.getMessage());
        }
    }

    public void deleteModuleBefore(String module, LocalDateTime cutoff) {
        log.warn("删除模块日志: module={}, cutoff={}", module, cutoff);
        try {
            repository.deleteByModuleAndTsBefore(module, cutoff);
        } catch (Exception ex) {
            log.warn("删除模块日志失败: {}", ex.getMessage());
        }
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
