package com.xy.lucky.logging.service;

import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.mapper.LogMapper;
import com.xy.lucky.logging.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogIngestService {
    private final LogRepository logRepository;
    private final LogMapper converter;
    private final LogAnalysisService analysisService;
    private final Sinks.Many<LogRecordVo> logSink;

    /**
     * 单条日志入库
     *
     * @param record 日志记录
     */
    public void ingest(LogRecordVo record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getTimestamp() == null) {
            record.setTimestamp(LocalDateTime.now());
        }
        LogPo po = converter.toPo(record);
        if (po != null) {
            try {
                logRepository.save(po);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("单条日志入库失败 id={}", record.getId(), e);
                }
            }
        }
        try {
            analysisService.aggregate(record);
        } finally {
            logSink.tryEmitNext(record);
        }
        if (log.isDebugEnabled()) {
            log.debug("摄取单条日志: id={}, module={}, level={}", record.getId(), record.getModule(), record.getLevel());
        }
    }

    /**
     * 批量日志入库
     *
     * @param records 日志记录
     */
    public void ingestBatch(List<LogRecordVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("摄取批量日志: count={}", records.size());
        }
        List<LogPo> buffer = new ArrayList<>(records.size());
        for (LogRecordVo r : records) {
            if (r.getId() == null) {
                r.setId(UUID.randomUUID().toString());
            }
            if (r.getTimestamp() == null) {
                r.setTimestamp(LocalDateTime.now());
            }
            LogPo po = converter.toPo(r);
            if (po != null) {
                buffer.add(po);
            }
        }
        if (!buffer.isEmpty()) {
            try {
                logRepository.saveAll(buffer);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("批量日志入库失败 size={}", buffer.size(), e);
                }
            }
        }
        for (LogRecordVo r : records) {
            try {
                analysisService.aggregate(r);
            } finally {
                logSink.tryEmitNext(r);
            }
        }
    }
}
