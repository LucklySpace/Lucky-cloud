package com.xy.lucky.logging.service;

import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.mapper.LogRecordConverter;
import com.xy.lucky.logging.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

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
    public List<LogRecordVo> query(String module, Instant start, Instant end, LogLevel level, int page, int size, String keyword) {
        log.info("执行日志查询: module={}, level={}, keyword={}", module, level, keyword);
        Instant actualStart = start != null ? start : Instant.EPOCH;
        Instant actualEnd = end != null ? end : Instant.now();
        int actualPage = Math.max(page, 0);
        int actualSize = Math.max(size, 1);
        int offset = actualPage * actualSize;

        List<LogPo> list = repository.queryRange(
                module,
                actualStart,
                actualEnd,
                level != null ? level.name() : null,
                keyword,
                offset,
                actualSize
        );
        return list.stream().map(converter::toVo).toList();
    }

    /**
     * 删除指定时间之前的日志
     *
     * @param cutoff 截止时间
     */
    public void deleteBefore(Instant cutoff) {
        log.warn("删除日志: cutoff={}", cutoff);
        repository.deleteByTsBefore(cutoff);
    }

    /**
     * 删除指定模块在指定时间之前的日志
     *
     * @param module 模块名
     * @param cutoff 截止时间
     */
    public void deleteModuleBefore(String module, Instant cutoff) {
        log.warn("删除模块日志: module={}, cutoff={}", module, cutoff);
        repository.deleteByModuleAndTsBefore(module, cutoff);
    }
}
