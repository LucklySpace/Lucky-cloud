package com.xy.lucky.logging.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xy.lucky.logging.domain.LogLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "LogRecordVo", description = "日志记录（用于采集与查询返回）")
public class LogRecordVo {

    @Schema(description = "日志ID（由服务端生成，UUID）", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "时间戳（ISO-8601，UTC）", example = "2025-12-24T08:30:15.123Z")
    private Instant timestamp;

    @NotNull(message = "请选择日志级别")
    @Schema(description = "日志级别", requiredMode = Schema.RequiredMode.REQUIRED, example = "ERROR")
    private LogLevel level;

    @NotBlank(message = "请输入模块名")
    @Schema(description = "模块名（业务域/子系统名）", requiredMode = Schema.RequiredMode.REQUIRED, example = "im-server")
    private String module;

    @NotBlank(message = "请输入服务名")
    @Schema(description = "服务名（微服务名）", requiredMode = Schema.RequiredMode.REQUIRED, example = "auth-service")
    private String service;

    @Schema(description = "来源地址（实例IP:端口 / hostname:端口）", example = "10.0.12.34:8080")
    private String address;

    @Size(max = 64, message = "traceId长度不能超过64")
    @Schema(description = "traceId（分布式链路追踪）", example = "0af7651916cd43dd8448eb211c80319c")
    private String traceId;

    @Size(max = 64, message = "spanId长度不能超过64")
    @Schema(description = "spanId（分布式链路追踪）", example = "b7ad6b7169203331")
    private String spanId;

    @Size(max = 100, message = "线程名长度不能超过100")
    @Schema(description = "线程名", example = "http-nio-8080-exec-12")
    private String thread;

    @NotBlank(message = "请输入日志内容")
    @Schema(description = "日志内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "order create failed")
    private String message;

    @Schema(description = "异常信息/堆栈（仅异常场景填）", example = "java.lang.IllegalStateException: x")
    private String exception;

    @Schema(description = "标签（用于快速过滤）", example = "[\"security\",\"audit\"]")
    private Set<String> tags;

    @Schema(description = "上下文（结构化字段，建议放 trace / biz / http 等信息）", example = "{\"orderId\":\"O-10001\",\"userId\":\"U-1\"}")
    private Map<String, Object> context;
}
