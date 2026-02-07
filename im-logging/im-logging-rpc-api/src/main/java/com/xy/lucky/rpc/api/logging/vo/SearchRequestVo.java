package com.xy.lucky.rpc.api.logging.vo;

import com.xy.lucky.rpc.api.logging.enums.LogLevel;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SearchRequestVo {
    private String module;
    private LocalDateTime start;
    private LocalDateTime end;
    private List<LogLevel> levels;
    private String keyword;
    private int from = 0;
    private int size = 20;
}
