package com.xy.lucky.rpc.api.logging.vo;

import com.xy.lucky.rpc.api.logging.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 日志记录VO（用于采集与查询返回）
 */
@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class LogRecordVo {

    /**
     * 日志ID（由服务端生成，UUID）
     */
    private String id;

    /**
     * 时间戳（ISO-8601，UTC）
     */
    private LocalDateTime timestamp;

    /**
     * 日志级别
     */
    private LogLevel level;

    /**
     * 模块名（业务域/子系统名）
     */
    private String module;

    /**
     * 服务名（微服务名）
     */
    private String service;

    /**
     * 来源地址（实例IP:端口 / hostname:端口）
     */
    private String address;

    /**
     * 环境
     */
    private String env;

    /**
     * traceId（分布式链路追踪）
     */
    private String traceId;

    /**
     * spanId（分布式链路追踪）
     */
    private String spanId;

    /**
     * 线程名
     */
    private String thread;

    /**
     * 日志内容
     */
    private String message;

    /**
     * 异常信息/堆栈（仅异常场景填）
     */
    private String exception;

    /**
     * 标签（用于快速过滤）
     */
    private Set<String> tags;

    /**
     * 上下文（结构化字段，建议放 trace / biz / http 等信息）
     */
    private Map<String, Object> context;
}
