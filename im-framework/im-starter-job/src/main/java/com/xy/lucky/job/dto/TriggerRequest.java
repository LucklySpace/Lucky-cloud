package com.xy.lucky.job.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TriggerRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 任务名称
     */
    private String jobName;

    /**
     * 任务参数
     */
    private String params;

    /**
     * 日志ID
     */
    private Long logId;

    /**
     * 请求时间戳
     */
    private Long timestamp;
}
