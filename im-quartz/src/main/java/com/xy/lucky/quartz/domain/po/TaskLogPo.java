package com.xy.lucky.quartz.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "im_quartz_task_log")
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "任务执行日志")
public class TaskLogPo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务名称")
    private String jobName;
    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "执行耗时(毫秒)")
    private Long executionTime; // ms

    @Schema(description = "是否成功")
    private Boolean success;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "异常信息")
    private String exceptionInfo;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "执行结果消息")
    private String resultMsg;

    @Schema(description = "执行进度(0-100)")
    private Integer progress;

    @Schema(description = "执行状态(0:运行中, 1:成功, 2:失败)")
    private Integer status;
}
