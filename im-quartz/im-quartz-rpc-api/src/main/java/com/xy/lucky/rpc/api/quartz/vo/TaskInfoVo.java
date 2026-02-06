package com.xy.lucky.rpc.api.quartz.vo;

import com.xy.lucky.rpc.api.quartz.enums.ConcurrencyStrategy;
import com.xy.lucky.rpc.api.quartz.enums.ScheduleType;
import com.xy.lucky.rpc.api.quartz.enums.TaskStatus;
import com.xy.lucky.rpc.api.quartz.enums.TriggerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务信息 VO
 *
 * @author Lucky
 */
@Data
@Schema(description = "任务信息VO")
public class TaskInfoVo implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "任务执行类全限定名或Bean名称")
    private String jobClass;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "执行间隔(毫秒)")
    private Long repeatInterval;

    @Schema(description = "调度类型")
    private ScheduleType scheduleType;

    @Schema(description = "任务状态")
    private TaskStatus status;

    @Schema(description = "并发策略")
    private ConcurrencyStrategy concurrencyStrategy;

    @Schema(description = "触发类型")
    private TriggerType triggerType;

    @Schema(description = "目标应用名称(Remote模式)")
    private String appName;

    @Schema(description = "任务处理器名称(Remote模式)")
    private String jobHandler;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "重试间隔(秒)")
    private Integer retryInterval;

    @Schema(description = "任务超时时间(秒)")
    private Integer timeout;

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Schema(description = "任务参数(JSON字符串)")
    private String jobData;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "下次执行时间")
    private LocalDateTime nextFireTime;

    @Schema(description = "上次执行时间")
    private LocalDateTime previousFireTime;
}
