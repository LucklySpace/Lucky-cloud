package com.xy.lucky.logging.disruptor;

import com.lmax.disruptor.EventHandler;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LogAnalysisHandler implements EventHandler<LogEvent> {
    private final LogAnalysisService analysisService;

    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) {
        LogRecordVo record = event.getRecord();
        if (record == null) {
            return;
        }
        try {
            analysisService.aggregate(record);
        } catch (Exception e) {
            log.error("aggregate log failed id={} msg={}", record.getId(), record.getMessage(), e);
        }
    }
}
