package com.xy.lucky.quartz.service;

import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.domain.enums.ConcurrencyStrategy;
import com.xy.lucky.quartz.domain.enums.ScheduleType;
import com.xy.lucky.quartz.domain.enums.TaskStatus;
import com.xy.lucky.quartz.job.ParallelJob;
import com.xy.lucky.quartz.job.SerialJob;
import com.xy.lucky.quartz.repository.TaskInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 定时任务服务类
 * 负责任务的增删改查以及Quartz调度的管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final Scheduler scheduler;
    private final TaskInfoRepository taskInfoRepository;

    /**
     * 添加任务
     *
     * @param taskInfoPo 任务信息
     */
    @Transactional
    public void addTask(TaskInfoPo taskInfoPo) {
        if (taskInfoRepository.existsByJobName(taskInfoPo.getJobName())) {
            throw new RuntimeException("Job name already exists");
        }
        taskInfoPo.setStatus(TaskStatus.STOPPED);
        taskInfoRepository.save(taskInfoPo);
    }

    /**
     * 更新任务
     * 如果任务正在运行，会重新调度
     *
     * @param taskInfoPo 任务信息
     * @throws SchedulerException 调度异常
     */
    @Transactional
    public void updateTask(TaskInfoPo taskInfoPo) throws SchedulerException {
        TaskInfoPo existing = taskInfoRepository.findById(taskInfoPo.getId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Copy properties
        existing.setDescription(taskInfoPo.getDescription());
        existing.setJobClass(taskInfoPo.getJobClass());
        existing.setCronExpression(taskInfoPo.getCronExpression());
        existing.setRepeatInterval(taskInfoPo.getRepeatInterval());
        existing.setScheduleType(taskInfoPo.getScheduleType());
        existing.setConcurrencyStrategy(taskInfoPo.getConcurrencyStrategy());
        existing.setJobData(taskInfoPo.getJobData());
        existing.setRetryCount(taskInfoPo.getRetryCount());
        existing.setRetryInterval(taskInfoPo.getRetryInterval());
        existing.setTimeout(taskInfoPo.getTimeout());
        existing.setAlarmEmail(taskInfoPo.getAlarmEmail());

        taskInfoRepository.save(existing);

        // Reschedule if running
        if (existing.getStatus() == TaskStatus.RUNNING) {
            deleteJob(existing);
            scheduleJob(existing);
        }
    }

    /**
     * 启动任务
     *
     * @param id 任务ID
     * @throws SchedulerException 调度异常
     */
    @Transactional
    public void startTask(Long id) throws SchedulerException {
        TaskInfoPo taskInfoPo = taskInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (taskInfoPo.getStatus() == TaskStatus.RUNNING) {
            return;
        }

        scheduleJob(taskInfoPo);
        taskInfoPo.setStatus(TaskStatus.RUNNING);
        taskInfoRepository.save(taskInfoPo);
    }

    /**
     * 停止任务
     *
     * @param id 任务ID
     * @throws SchedulerException 调度异常
     */
    @Transactional
    public void stopTask(Long id) throws SchedulerException {
        TaskInfoPo taskInfoPo = taskInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        deleteJob(taskInfoPo);
        taskInfoPo.setStatus(TaskStatus.STOPPED);
        taskInfoRepository.save(taskInfoPo);
    }

    /**
     * 删除任务
     *
     * @param id 任务ID
     * @throws SchedulerException 调度异常
     */
    @Transactional
    public void deleteTask(Long id) throws SchedulerException {
        TaskInfoPo taskInfoPo = taskInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        deleteJob(taskInfoPo);
        taskInfoRepository.delete(taskInfoPo);
    }

    /**
     * 立即触发一次任务
     *
     * @param id 任务ID
     * @throws SchedulerException 调度异常
     */
    public void triggerTask(Long id) throws SchedulerException {
        TaskInfoPo taskInfoPo = taskInfoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        JobKey jobKey = JobKey.jobKey(taskInfoPo.getJobName(), taskInfoPo.getJobGroup());

        if (scheduler.checkExists(jobKey)) {
            scheduler.triggerJob(jobKey);
        } else {
            // 如果任务未运行（未在调度器中），则临时创建一个JobDetail来执行一次
            // 注意：这种方式不会影响任务的原始状态（STOPPED）
            log.info("Task {} is not scheduled, triggering via temporary job", taskInfoPo.getJobName());
            triggerOnce(taskInfoPo);
        }
    }

    /**
     * 临时执行一次任务（不改变任务状态）
     */
    private void triggerOnce(TaskInfoPo taskInfoPo) throws SchedulerException {
        Class<? extends Job> jobClass = taskInfoPo.getConcurrencyStrategy() == ConcurrencyStrategy.SERIAL
                ? SerialJob.class : ParallelJob.class;

        // 使用唯一的Identity避免冲突
        String tempJobName = taskInfoPo.getJobName() + "_trigger_" + System.currentTimeMillis();

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(tempJobName, taskInfoPo.getJobGroup())
                .usingJobData("taskId", taskInfoPo.getId())
                .storeDurably(false) // 执行完后自动删除
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(tempJobName + "_trigger", taskInfoPo.getJobGroup())
                .startNow()
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 调度任务
     */
    private void scheduleJob(TaskInfoPo taskInfoPo) throws SchedulerException {
        Class<? extends Job> jobClass = taskInfoPo.getConcurrencyStrategy() == ConcurrencyStrategy.SERIAL
                ? SerialJob.class : ParallelJob.class;

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(taskInfoPo.getJobName(), taskInfoPo.getJobGroup())
                .usingJobData("taskId", taskInfoPo.getId())
                .requestRecovery(true) // 开启故障转移
                .build();

        Trigger trigger;
        if (taskInfoPo.getScheduleType() == ScheduleType.CRON) {
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(taskInfoPo.getJobName(), taskInfoPo.getJobGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(taskInfoPo.getCronExpression()))
                    .build();
        } else {
            trigger = TriggerBuilder.newTrigger()
                    .withIdentity(taskInfoPo.getJobName(), taskInfoPo.getJobGroup())
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(taskInfoPo.getRepeatInterval())
                            .repeatForever())
                    .build();
        }

        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * 删除Quartz中的Job
     */
    private void deleteJob(TaskInfoPo taskInfoPo) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(taskInfoPo.getJobName(), taskInfoPo.getJobGroup());
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    /**
     * 查询所有任务
     */
    public List<TaskInfoPo> findAll() {
        return taskInfoRepository.findAll();
    }

    /**
     * 根据ID查询任务
     */
    public TaskInfoPo findById(Long id) {
        return taskInfoRepository.findById(id).orElse(null);
    }
}
