package com.xy.lucky.logging.disruptor;

import com.lmax.disruptor.EventHandler;
import com.xy.lucky.logging.domain.po.LogPo;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.mapper.LogRecordConverter;
import com.xy.lucky.logging.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 日志存储处理器
 * 使用批量入库优化性能
 */
@Slf4j
@RequiredArgsConstructor
public class LogStoreHandler implements EventHandler<LogEvent> {
    private static final int BATCH_SIZE = 1000;
    private final LogRepository logRepository;
    private final LogRecordConverter converter;
    private final List<LogPo> buffer = new ArrayList<>(BATCH_SIZE);

    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) {
        LogRecordVo record = event.getRecord();
        if (record != null) {
            LogPo po = converter.toPo(record);
            if (po != null) {
                buffer.add(po);
            }
        }

        if (endOfBatch || buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    private void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        try {
            logRepository.saveAll(buffer);
            if (log.isDebugEnabled()) {
                log.debug("Batch stored {} logs", buffer.size());
            }
        } catch (Exception e) {
            log.error("Batch store logs failed, size={}", buffer.size(), e);
        } finally {
            buffer.clear();
        }
    }
}
