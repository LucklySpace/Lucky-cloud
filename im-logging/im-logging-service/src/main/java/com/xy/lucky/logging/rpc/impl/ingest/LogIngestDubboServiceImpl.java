package com.xy.lucky.logging.rpc.impl.ingest;

import com.xy.lucky.logging.service.LogIngestService;
import com.xy.lucky.rpc.api.logging.enums.LogLevel;
import com.xy.lucky.rpc.api.logging.ingest.LogIngestDubboService;
import com.xy.lucky.rpc.api.logging.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 日志采集Dubbo服务实现
 */
@Slf4j
@DubboService
@RequiredArgsConstructor
public class LogIngestDubboServiceImpl implements LogIngestDubboService {

    private final LogIngestService logIngestService;

    @Override
    public void ingest(LogRecordVo record) {
        LogRecordVo vo = toVo(record);
        logIngestService.ingest(vo);
    }

    @Override
    public void ingestBatch(List<LogRecordVo> records) {
        List<LogRecordVo> vos = records.stream()
                .map(this::toVo)
                .toList();
        logIngestService.ingestBatch(vos);
    }

    private LogRecordVo toVo(LogRecordVo rpcVo) {
        if (rpcVo == null) {
            return null;
        }

        LogLevel level = null;
        if (rpcVo.getLevel() != null) {
            level = LogLevel.valueOf(rpcVo.getLevel().name());
        }

        return LogRecordVo.builder()
                .id(rpcVo.getId() != null ? rpcVo.getId() : UUID.randomUUID().toString())
                .timestamp(rpcVo.getTimestamp() != null ? rpcVo.getTimestamp() : LocalDateTime.now())
                .level(level)
                .module(rpcVo.getModule())
                .service(rpcVo.getService())
                .address(rpcVo.getAddress())
                .env(rpcVo.getEnv())
                .traceId(rpcVo.getTraceId())
                .spanId(rpcVo.getSpanId())
                .thread(rpcVo.getThread())
                .message(rpcVo.getMessage())
                .exception(rpcVo.getException())
                .tags(rpcVo.getTags())
                .context(rpcVo.getContext())
                .build();
    }
}
