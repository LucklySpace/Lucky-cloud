package com.xy.lucky.rpc.api.logging.dto;

import com.xy.lucky.rpc.api.logging.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日志查询DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogQueryDto {

    /**
     * 模块名
     */
    private String module;

    /**
     * 开始时间
     */
    private LocalDateTime start;

    /**
     * 结束时间
     */
    private LocalDateTime end;

    /**
     * 日志级别
     */
    private LogLevel level;

    /**
     * 服务名
     */
    private String service;

    /**
     * 环境
     */
    private String env;

    /**
     * 页码（从1开始）
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;

    /**
     * 关键字
     */
    private String keyword;
}
