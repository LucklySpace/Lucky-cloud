package com.xy.lucky.ai.tools.time;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Slf4j
@Component
public class DateTimeTool {


    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        log.info("getCurrentDateTime 工具被调用");
        return LocalDateTime.now()
                .atZone(LocaleContextHolder.getTimeZone().toZoneId())
                .toString();
    }


}