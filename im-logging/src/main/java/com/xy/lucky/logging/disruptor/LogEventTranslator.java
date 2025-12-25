package com.xy.lucky.logging.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.xy.lucky.logging.domain.vo.LogRecordVo;

public class LogEventTranslator implements EventTranslatorOneArg<LogEvent, LogRecordVo> {
    @Override
    public void translateTo(LogEvent event, long sequence, LogRecordVo arg0) {
        event.setRecord(arg0);
    }
}
