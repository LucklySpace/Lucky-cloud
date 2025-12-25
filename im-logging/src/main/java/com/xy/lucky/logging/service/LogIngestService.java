package com.xy.lucky.logging.service;

import com.xy.lucky.logging.disruptor.LogPublisher;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 日志摄取服务
 * 负责接收日志记录并发布到Disruptor队列进行异步处理，确保高性能摄取
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogIngestService {
    private final LogPublisher publisher;

    /**
     * 摄取单条日志记录
     * 如果未设置ID或时间戳，会自动生成
     *
     * @param record 日志记录VO
     */
    public void ingest(LogRecordVo record) {
        if (record.getId() == null) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getTimestamp() == null) {
            record.setTimestamp(Instant.now());
        }
        publisher.publish(record);
        log.info("摄取单条日志: id={}, module={}, level={}", record.getId(), record.getModule(), record.getLevel());
    }

    /**
     * 摄取批量日志记录
     * 对每条记录调用单条摄取方法
     *
     * @param records 日志记录列表
     */
    public void ingestBatch(List<LogRecordVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        log.info("摄取批量日志: count={}", records.size());
        for (LogRecordVo r : records) {
            ingest(r);
        }
    }
}