package com.xy.lucky.database.web;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;


/**
 * 数据库启动健康校验
 * <p>
 * 使用 ApplicationRunner，在应用启动后同步尝试连接数据库，
 * 手动实现重试逻辑
 */
@Slf4j
@Component
public class DatabaseInitializer implements ApplicationRunner {

    @Value("${database.init.max-attempts:5}")
    private int maxAttempts;

    @Value("${database.init.retry-delay-ms:5000}")
    private long retryDelayMs;

    @Resource
    private DataSource dataSource;

    /**
     * 应用启动后执行，尝试连接数据库，失败则按间隔重试，重试耗尽后抛出异常阻止应用继续启动
     */
    @Override
    public void run(ApplicationArguments args) {
        int maxRetry = Math.max(1, maxAttempts);
        long delayMs = Math.max(100L, retryDelayMs);
        int attempt = 0;
        while (attempt < maxRetry) {
            attempt++;
            try (Connection conn = dataSource.getConnection()) {
                log.info("[DatabaseInitializer] Database connection successful (URL: {})", conn.getMetaData().getURL());
                return;
            } catch (Exception e) {
                log.warn("[DatabaseInitializer] Attempt {}/{} - Database not ready, retrying in {} ms...", attempt, maxRetry, delayMs);
                if (attempt >= maxRetry) {
                    log.error("[DatabaseInitializer] Reached max retry attempts ({}), failing startup", maxRetry);
                    throw new IllegalStateException("Unable to connect to database after " + maxRetry + " attempts", e);
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Database initialization interrupted", ie);
                }
            }
        }
    }
}
