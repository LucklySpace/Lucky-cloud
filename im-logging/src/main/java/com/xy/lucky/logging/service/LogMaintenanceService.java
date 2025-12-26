package com.xy.lucky.logging.service;

import com.xy.lucky.logging.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogMaintenanceService {

    private final LogRepository repository;

    @Value("${logging.retention.days:7}")
    private int retentionDays;

    /**
     * 定时清理过期的日志
     */
    @Scheduled(fixedDelayString = "${logging.retention.cleanup-interval-ms:300000}")
    public void cleanupExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minus(retentionDays, ChronoUnit.DAYS);
        repository.deleteByTsBefore(cutoff);
        log.info("log cleanup finished cutoff={} retentionDays={}", cutoff, retentionDays);
    }
}
