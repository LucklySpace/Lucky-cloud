package com.xy.lucky.logging.disruptor;

import com.lmax.disruptor.dsl.Disruptor;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogPublisher {
    private final Disruptor<LogEvent> disruptor;
    private final LogEventTranslator translator = new LogEventTranslator();

    public void publish(LogRecordVo record) {
        disruptor.getRingBuffer().publishEvent(translator, record);
    }
}
