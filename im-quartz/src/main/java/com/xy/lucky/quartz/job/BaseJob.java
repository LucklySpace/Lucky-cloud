package com.xy.lucky.quartz.job;

import com.xy.lucky.quartz.domain.context.ShardingContext;
import com.xy.lucky.quartz.domain.context.TaskLogger;
import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.po.TaskLogPo;
import com.xy.lucky.quartz.manager.ClusterManager;
import com.xy.lucky.quartz.repository.TaskInfoRepository;
import com.xy.lucky.quartz.repository.TaskLogRepository;
import com.xy.lucky.quartz.service.AlarmService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base Job
 */
@Slf4j
public abstract class BaseJob extends QuartzJobBean {

    @Autowired
    private TaskInfoRepository taskInfoRepository;

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private ClusterManager clusterManager;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    @Qualifier("jobExecutorService")
    private ExecutorService executorService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long taskId = context.getMergedJobDataMap().getLong("taskId");
        TaskInfoPo taskInfoPo = taskInfoRepository.findById(taskId).orElse(null);

        if (taskInfoPo == null) {
            log.warn("TaskInfo not found for taskId: {}", taskId);
            return;
        }

        long startTime = System.currentTimeMillis();
        TaskLogPo taskLogPo = new TaskLogPo();
        taskLogPo.setJobName(taskInfoPo.getJobName());
        taskLogPo.setJobGroup(taskInfoPo.getJobGroup());
        taskLogPo.setStartTime(LocalDateTime.now());
        taskLogPo.setProgress(0);
        taskLogPo.setStatus(0); // Running
        taskLogPo = taskLogRepository.save(taskLogPo); // Save immediately to get ID

        // Retry & Timeout Config
        int maxRetries = taskInfoPo.getRetryCount() != null ? taskInfoPo.getRetryCount() : 0;
        int retryInterval = taskInfoPo.getRetryInterval() != null ? taskInfoPo.getRetryInterval() : 10;
        int timeout = taskInfoPo.getTimeout() != null ? taskInfoPo.getTimeout() : 0;

        int attempt = 0;
        boolean success = false;
        Exception lastException = null;

        try {
            log.info("Starting task: {}", taskInfoPo.getJobName());

            // Prepare context
            int[] shardingInfo = clusterManager.getShardingInfo();
            ShardingContext shardingContext = new ShardingContext(shardingInfo[0], shardingInfo[1]);

            // Prepare logger
            TaskLogPo finalTaskLogPo = taskLogPo;
            TaskLogger logger = new TaskLogger() {
                @Override
                public void log(int percent, String msg) {
                    finalTaskLogPo.setProgress(percent);
                    finalTaskLogPo.setResultMsg(msg);
                    taskLogRepository.save(finalTaskLogPo);
                }

                @Override
                public void log(String msg) {
                    finalTaskLogPo.setResultMsg(msg);
                    taskLogRepository.save(finalTaskLogPo);
                }
            };

            // Execute with retry and timeout
            while (attempt <= maxRetries && !success) {
                if (attempt > 0) {
                    logger.log("重试第 " + attempt + " 次...");
                    try {
                        TimeUnit.SECONDS.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JobExecutionException("Retry interrupted", ie);
                    }
                }

                try {
                    if (timeout > 0) {
                        Future<?> future = executorService.submit(() -> {
                            try {
                                executeTask(taskInfoPo, shardingContext, logger);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        future.get(timeout, TimeUnit.SECONDS);
                    } else {
                        executeTask(taskInfoPo, shardingContext, logger);
                    }
                    success = true;
                } catch (Exception e) {
                    lastException = e;
                    if (e instanceof TimeoutException) {
                        log.error("Task timeout (attempt {}/{})", attempt, maxRetries);
                        logger.log("执行超时");
                    } else {
                        log.error("Task execution failed (attempt {}/{})", attempt, maxRetries, e);
                        logger.log("执行失败: " + e.getMessage());
                    }
                }
                attempt++;
            }

            if (success) {
                taskLogPo.setSuccess(true);
                taskLogPo.setStatus(1); // Success
                taskLogPo.setProgress(100);
                if (taskLogPo.getResultMsg() == null || !taskLogPo.getResultMsg().contains("Success")) {
                    taskLogPo.setResultMsg("Success");
                }
            } else {
                throw lastException != null ? lastException : new RuntimeException("Task failed after " + attempt + " attempts");
            }

        } catch (Exception e) {
            log.error("Task failed: {}", taskInfoPo.getJobName(), e);
            taskLogPo.setSuccess(false);
            taskLogPo.setStatus(2); // Failed
            taskLogPo.setExceptionInfo(e.toString());

            // Send Alarm Email
            if (taskInfoPo.getAlarmEmail() != null && !taskInfoPo.getAlarmEmail().isEmpty()) {
                String subject = "任务失败告警: " + taskInfoPo.getJobName();
                String content = "任务ID: " + taskInfoPo.getId() + "\n" +
                        "任务名称: " + taskInfoPo.getJobName() + "\n" +
                        "错误信息: " + e.toString();
                alarmService.sendAlarm(taskInfoPo.getAlarmEmail(), subject, content);
            }
        } finally {
            long endTime = System.currentTimeMillis();
            taskLogPo.setEndTime(LocalDateTime.now());
            taskLogPo.setExecutionTime(endTime - startTime);
            taskLogRepository.save(taskLogPo);
        }
    }

    protected abstract void executeTask(TaskInfoPo taskInfoPo, ShardingContext context, TaskLogger logger) throws Exception;
}
