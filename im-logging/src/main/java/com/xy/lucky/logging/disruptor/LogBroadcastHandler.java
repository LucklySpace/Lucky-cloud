package com.xy.lucky.logging.disruptor;

import com.lmax.disruptor.EventHandler;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@RequiredArgsConstructor
public class LogBroadcastHandler implements EventHandler<LogEvent> {

    private final SimpMessagingTemplate template;

    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) {
        LogRecordVo record = event.getRecord();
        if (record != null) {
            template.convertAndSend("/topic/logs", record);
        }
    }
}
