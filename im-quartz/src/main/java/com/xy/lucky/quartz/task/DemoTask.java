package com.xy.lucky.quartz.task;

import com.xy.lucky.quartz.domain.context.ShardingContext;
import com.xy.lucky.quartz.domain.context.TaskLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 演示任务
 * 展示分片处理和进度上报
 */
@Slf4j
@Component("demoTask")
public class DemoTask implements ITask {

    @Override
    public void execute(String params, ShardingContext context, TaskLogger logger) {
        log.info("Demo Task executing with params: {}, sharding: {}/{}",
                params, context.getShardingItem(), context.getShardingTotalCount());

        try {
            // 模拟进度上报
            for (int i = 0; i <= 100; i += 20) {
                if (shouldProcess(i, context)) {
                    log.info("Processing item {} on sharding {}", i, context.getShardingItem());
                    logger.log(i, "正在处理第 " + i + "% 的数据...");
                    TimeUnit.MILLISECONDS.sleep(500);
                }
            }
            logger.log(100, "任务执行完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(0, "任务被中断");
        }
        log.info("Demo Task finished.");
    }

    /**
     * 简单的分片逻辑示例
     */
    private boolean shouldProcess(int item, ShardingContext context) {
        // 实际业务中通常是根据ID取模
        return item % context.getShardingTotalCount() == context.getShardingItem();
    }
}
