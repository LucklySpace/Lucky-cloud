package com.xy.lucky.logging.rpc.impl.query;

import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.mapper.LogMapper;
import com.xy.lucky.logging.repository.LogRepository;
import com.xy.lucky.rpc.api.logging.dto.LogQueryDto;
import com.xy.lucky.rpc.api.logging.dto.PageResult;
import com.xy.lucky.rpc.api.logging.query.LogQueryDubboService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日志查询Dubbo服务实现
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class LogQueryDubboServiceImpl implements LogQueryDubboService {

    private final LogRepository repository;
    private final LogMapper logMapper;

    @Override
    public PageResult<RpcLogRecordVo> query(String module, LocalDateTime start, LocalDateTime end,
                                            RpcLogLevel level, String service, String env,
                                            int page, int size, String keyword) {
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
            Page<LogPo> pageData = repository.findAll(
                    getSpecFromDatesAndExample(actualStart, actualEnd, example),
                    PageRequest.of(actualPage - 1, actualSize, Sort.by(Sort.Direction.DESC, "ts"))
            );

            List<RpcLogRecordVo> list = pageData.getContent().stream()
                    .map(logMapper::toVo)
                    .map(this::toRpcVo)
                    .collect(Collectors.toList());

            return PageResult.<RpcLogRecordVo>builder()
                    .content(list)
                    .pageNumber(pageData.getPageable().getPageNumber())
                    .pageSize(pageData.getPageable().getPageSize())
                    .totalElements(pageData.getTotalElements())
                    .totalPages(pageData.getTotalPages())
                    .hasNext(pageData.hasNext())
                    .hasPrevious(pageData.hasPrevious())
                    .build();
        } catch (Exception ex) {
            log.warn("查询日志失败，返回空列表: {}", ex.getMessage());
            return PageResult.<RpcLogRecordVo>builder()
                    .content(List.of())
                    .pageNumber(page - 1)
                    .pageSize(size)
                    .totalElements(0L)
                    .totalPages(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }
    }

    @Override
    public PageResult<RpcLogRecordVo> query(LogQueryDto queryDto) {
        if (queryDto == null) {
            return PageResult.<RpcLogRecordVo>builder()
                    .content(List.of())
                    .pageNumber(0)
                    .pageSize(10)
                    .totalElements(0L)
                    .totalPages(0)
                    .hasNext(false)
                    .hasPrevious(false)
                    .build();
        }

        return query(
                queryDto.getModule(),
                queryDto.getStart(),
                queryDto.getEnd(),
                queryDto.getLevel(),
                queryDto.getService(),
                queryDto.getEnv(),
                queryDto.getPage() != null ? queryDto.getPage() : 1,
                queryDto.getSize() != null ? queryDto.getSize() : 10,
                queryDto.getKeyword()
        );
    }

    @Override
    public Map<String, Long> histogram(String module, LocalDateTime start, LocalDateTime end,
                                       RpcLogLevel level, String service, String env,
                                       String keyword, String interval) {
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

    @Override
    public List<String> listServices(String env) {
        // 此方法需要从服务发现获取，暂时返回空列表
        return List.of();
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void deleteBefore(LocalDateTime cutoff) {
        log.warn("删除日志: cutoff={}", cutoff);
        try {
            repository.deleteByTsBefore(cutoff);
        } catch (Exception ex) {
            log.warn("删除日志失败: {}", ex.getMessage());
        }
    }

    @Override
    public void deleteModuleBefore(String module, LocalDateTime cutoff) {
        log.warn("删除模块日志: module={}, cutoff={}", module, cutoff);
        try {
            repository.deleteByModuleAndTsBefore(module, cutoff);
        } catch (Exception ex) {
            log.warn("删除模块日志失败: {}", ex.getMessage());
        }
    }

    private Specification<LogPo> getSpecFromDatesAndExample(
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

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private RpcLogRecordVo toRpcVo(LogRecordVo vo) {
        if (vo == null) {
            return null;
        }
        return RpcLogRecordVo.builder()
                .id(vo.getId())
                .timestamp(vo.getTimestamp())
                .level(vo.getLevel() != null ? RpcLogLevel.valueOf(vo.getLevel().name()) : null)
                .module(vo.getModule())
                .service(vo.getService())
                .address(vo.getAddress())
                .env(vo.getEnv())
                .traceId(vo.getTraceId())
                .spanId(vo.getSpanId())
                .thread(vo.getThread())
                .message(vo.getMessage())
                .exception(vo.getException())
                .tags(vo.getTags())
                .context(vo.getContext())
                .build();
    }
}
