package com.xy.lucky.quartz.domain.vo;

import com.xy.lucky.quartz.domain.enums.ConcurrencyStrategy;
import com.xy.lucky.quartz.domain.enums.ScheduleType;
import com.xy.lucky.quartz.domain.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务信息VO")
public class TaskInfoVo {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务名称")
    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    @Schema(description = "任务分组")
    @NotBlank(message = "任务分组不能为空")
    private String jobGroup;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "任务执行类全限定名或Bean名称")
    @NotBlank(message = "执行类不能为空")
    private String jobClass;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "执行间隔(毫秒)")
    private Long repeatInterval;

    @Schema(description = "调度类型")
    @NotNull(message = "调度类型不能为空")
    private ScheduleType scheduleType;

    @Schema(description = "任务状态")
    private TaskStatus status;

    @Schema(description = "并发策略")
    @NotNull(message = "并发策略不能为空")
    private ConcurrencyStrategy concurrencyStrategy;

    @Schema(description = "重试次数")
    private Integer retryCount = 0;

    @Schema(description = "重试间隔(秒)")
    private Integer retryInterval = 10;

    @Schema(description = "任务超时时间(秒)")
    private Integer timeout = 0;

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Schema(description = "任务参数(JSON字符串)")
    private String jobData;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
