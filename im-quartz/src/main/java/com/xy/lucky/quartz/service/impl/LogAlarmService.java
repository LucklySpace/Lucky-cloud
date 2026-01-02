package com.xy.lucky.quartz.service.impl;

import com.xy.lucky.quartz.service.AlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogAlarmService implements AlarmService {

    @Override
    public boolean sendAlarm(String email, String subject, String content) {
        // TODO: 集成邮件服务 (spring-boot-starter-mail)
        // 目前仅打印日志模拟报警
        log.error("【ALARM】To: {}, Subject: {}, Content: {}", email, subject, content);
        return true;
    }
}
