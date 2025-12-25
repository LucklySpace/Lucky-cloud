package com.xy.lucky.logging.disruptor;

import com.lmax.disruptor.EventFactory;

public class LogEventFactory implements EventFactory<LogEvent> {
    @Override
    public LogEvent newInstance() {
        return new LogEvent();
    }
}
