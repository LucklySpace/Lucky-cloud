package com.xy.lucky.database;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 最大重试次数
     */
    private static final int MAX_ATTEMPTS = 5;
    /**
     * 重试间隔毫秒
     */
    private static final long RETRY_DELAY_MS = 5000L;

    @Resource
    private DataSource dataSource;

    /**
     * 应用启动后执行，尝试连接数据库，失败则按间隔重试，重试耗尽后抛出异常阻止应用继续启动
     */
    @Override
    public void run(ApplicationArguments args) {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try (Connection conn = dataSource.getConnection()) {
                log.info("[DatabaseInitializer] Database connection successful (URL: {})", conn.getMetaData().getURL());
                return;
            } catch (Exception e) {
                log.warn("[DatabaseInitializer] Attempt {}/{} - Database not ready, retrying in {} ms...", attempt, MAX_ATTEMPTS, RETRY_DELAY_MS);
                if (attempt >= MAX_ATTEMPTS) {
                    log.error("[DatabaseInitializer] Reached max retry attempts ({}), failing startup", MAX_ATTEMPTS);
                    throw new IllegalStateException("Unable to connect to database after " + MAX_ATTEMPTS + " attempts", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Database initialization interrupted", ie);
                }
            }
        }
    }
}
