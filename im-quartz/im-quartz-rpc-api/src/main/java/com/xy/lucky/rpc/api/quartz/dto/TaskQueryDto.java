package com.xy.lucky.rpc.api.quartz.dto;

import com.xy.lucky.rpc.api.quartz.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务查询 DTO
 *
 * @author Lucky
 */
@Data
@Schema(description = "任务查询DTO")
public class TaskQueryDto implements Serializable {

    @Schema(description = "任务名称(模糊查询)")
    private String jobName;

    @Schema(description = "任务分组")
    private String jobGroup;

    @Schema(description = "任务状态")
    private TaskStatus status;

    @Schema(description = "触发类型")
    private String triggerType;

    @Schema(description = "创建时间开始")
    private LocalDateTime createdTimeStart;

    @Schema(description = "创建时间结束")
    private LocalDateTime createdTimeEnd;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段", example = "createdTime")
    private String sortField = "createdTime";

    @Schema(description = "排序方向", example = "DESC")
    private String sortOrder = "DESC";
}
