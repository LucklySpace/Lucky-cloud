package com.xy.lucky.quartz.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务日志VO")
public class TaskLogVo {

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
    private Long executionTime;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "异常信息")
    private String exceptionInfo;

    @Schema(description = "执行结果消息")
    private String resultMsg;

    @Schema(description = "执行进度(0-100)")
    private Integer progress;

    @Schema(description = "执行状态(0:运行中, 1:成功, 2:失败)")
    private Integer status;
}
