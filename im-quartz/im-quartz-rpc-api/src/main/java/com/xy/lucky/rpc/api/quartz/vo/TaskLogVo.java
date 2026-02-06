package com.xy.lucky.rpc.api.quartz.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务日志 VO
 *
 * @author Lucky
 */
@Data
@Schema(description = "任务日志VO")
public class TaskLogVo implements Serializable {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "任务ID")
    private Long taskId;

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

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "执行节点")
    private String executionNode;
}
