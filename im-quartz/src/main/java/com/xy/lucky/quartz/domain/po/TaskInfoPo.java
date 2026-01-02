package com.xy.lucky.quartz.domain.po;

import com.xy.lucky.quartz.domain.enums.ConcurrencyStrategy;
import com.xy.lucky.quartz.domain.enums.ScheduleType;
import com.xy.lucky.quartz.domain.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_quartz_task_info")
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "任务信息实体")
public class TaskInfoPo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "主键ID")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "任务名称")
    private String jobName;

    @Column(nullable = false)
    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "任务描述")
    private String description;

    @Column(nullable = false)
    @Schema(description = "任务执行类全限定名或Bean名称")
    private String jobClass;

    @Schema(description = "Cron表达式")
    private String cronExpression;

    @Schema(description = "执行间隔(毫秒)")
    private Long repeatInterval;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "调度类型(CRON/FIXED_RATE/FIXED_DELAY)")
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "任务状态(RUNNING/PAUSED/STOPPED)")
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "并发策略(SERIAL/PARALLEL)")
    private ConcurrencyStrategy concurrencyStrategy;

    @Schema(description = "重试次数")
    private Integer retryCount = 0;

    @Schema(description = "重试间隔(秒)")
    private Integer retryInterval = 10;

    @Schema(description = "任务超时时间(秒), 0表示不超时")
    private Integer timeout = 0;

    @Schema(description = "报警邮件")
    private String alarmEmail;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "任务参数(JSON)")
    private String jobData;

    @CreatedDate
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @LastModifiedDate
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
