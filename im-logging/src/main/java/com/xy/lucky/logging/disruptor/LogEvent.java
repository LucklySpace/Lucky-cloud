package com.xy.lucky.logging.disruptor;

import com.xy.lucky.logging.domain.vo.LogRecordVo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogEvent {
    private LogRecordVo record;
}
