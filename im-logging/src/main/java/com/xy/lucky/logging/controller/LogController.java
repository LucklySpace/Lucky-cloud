package com.xy.lucky.logging.controller;

import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.service.LogAnalysisService;
import com.xy.lucky.logging.service.LogIngestService;
import com.xy.lucky.logging.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 日志服务控制器
 * 提供日志的采集、查询、统计与维护接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "logging", description = "日志采集、管理与分析接口")
public class LogController {

    private final LogIngestService ingestService;
    private final LogQueryService queryService;
    private final LogAnalysisService analysisService;

    /**
     * 采集单条日志
     *
     * @param record 日志记录
     * @return 日志ID
     */
    @Operation(summary = "采集单条日志", description = "接收并异步存储单条日志记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "接收成功（返回日志ID）",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(responseCode = "400", description = "参数校验失败")
    })
    @PostMapping
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "日志记录。`id`、`timestamp` 可不传由服务端生成；`exception` 仅异常场景传。",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LogRecordVo.class),
                    examples = @ExampleObject(
                            name = "errorLog",
                            value = """
                                    {
                                      "level": "ERROR",
                                      "module": "im-server",
                                      "service": "order-service",
                                      "address": "10.0.12.34:8080",
                                      "traceId": "0af7651916cd43dd8448eb211c80319c",
                                      "spanId": "b7ad6b7169203331",
                                      "thread": "http-nio-8080-exec-12",
                                      "message": "order create failed",
                                      "exception": "java.lang.IllegalStateException: x",
                                      "tags": ["biz","order"],
                                      "context": {"orderId": "O-10001", "userId": "U-1"}
                                    }
                                    """
                    )
            )
    )
    public String ingest(@RequestBody @Valid LogRecordVo record) {
        log.info("接收日志采集请求: module={} level={}", record.getModule(), record.getLevel());
        ingestService.ingest(record);
        return record.getId();
    }

    /**
     * 批量采集日志
     *
     * @param records 日志记录列表
     * @return 成功接收的数量
     */
    @Operation(summary = "批量采集日志", description = "接收并异步存储多条日志记录，适合高吞吐场景")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "接收成功（返回接收数量）",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Integer.class))
            ),
            @ApiResponse(responseCode = "400", description = "参数校验失败")
    })
    @PostMapping("/batch")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "日志记录列表（建议单批 100~1000 条），字段含义同单条采集。",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = LogRecordVo.class))
            )
    )
    public int ingestBatch(@RequestBody @Valid List<LogRecordVo> records) {
        int size = records != null ? records.size() : 0;
        log.info("接收批量日志采集请求: count={}", size);
        ingestService.ingestBatch(records);
        return size;
    }

    /**
     * 查询日志
     *
     * @param module  模块名
     * @param start   开始时间
     * @param end     结束时间
     * @param level   日志级别
     * @param page    页码 (0开始)
     * @param size    每页大小
     * @param keyword 关键字查询
     * @return 日志列表
     */
    @Operation(summary = "查询日志", description = "根据条件分页查询日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = LogRecordVo.class))
                    )
            )
    })
    @GetMapping
    public List<LogRecordVo> query(
            @Parameter(description = "模块名（不传则不过滤）", example = "im-server") @RequestParam(required = false) String module,
            @Parameter(description = "开始时间（ISO-8601，不传默认从1970开始）", example = "2025-12-24T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "结束时间（ISO-8601，不传默认当前时间）", example = "2025-12-24T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @Parameter(description = "日志级别（不传则不过滤）", example = "ERROR") @RequestParam(required = false) LogLevel level,
            @Parameter(description = "页码（从0开始）", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键字（对message做包含匹配）", example = "timeout") @RequestParam(required = false) String keyword) {
        log.info("查询日志请求: module={} level={} keyword={}", module, level, keyword);
        return queryService.query(module, start, end, level, page, size, keyword);
    }

    /**
     * 统计概览
     *
     * @return 统计数据
     */
    @Operation(summary = "统计概览", description = "获取日志统计概览数据，包括各级别日志数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    @GetMapping("/stats/overview")
    public Map<String, Object> overview() {
        return analysisService.overview();
    }

    /**
     * 按小时统计序列
     *
     * @param level 日志级别
     * @param hours 统计最近多少小时
     * @return 时间序列数据
     */
    @Operation(summary = "按小时统计序列", description = "获取指定级别日志在最近N小时内的数量趋势")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "获取成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            )
    })
    @GetMapping("/stats/hourly")
    public Map<String, Long> hourly(
            @Parameter(description = "日志级别") @RequestParam(defaultValue = "ERROR") String level,
            @Parameter(description = "最近小时数") @RequestParam(defaultValue = "24") int hours) {
        return analysisService.hourlySeries(level, hours);
    }

    /**
     * 删除指定时间之前的日志
     *
     * @param cutoff 截止时间
     * @return 操作结果
     */
    @Operation(summary = "清理日志", description = "删除指定时间之前的所有日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
            )
    })
    @DeleteMapping("/before")
    public String deleteBefore(@Parameter(description = "截止时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cutoff) {
        log.warn("执行日志清理: cutoff={}", cutoff);
        queryService.deleteBefore(cutoff);
        return "ok";
    }

    /**
     * 删除指定模块在指定时间之前的日志
     *
     * @param module 模块名
     * @param cutoff 截止时间
     * @return 操作结果
     */
    @Operation(summary = "清理模块日志", description = "删除指定模块在指定时间之前的所有日志")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "清理成功",
                    content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
            )
    })
    @DeleteMapping("/module/{module}/before")
    public String deleteModuleBefore(
            @Parameter(description = "模块名") @PathVariable String module,
            @Parameter(description = "截止时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cutoff) {
        log.warn("执行模块日志清理: module={} cutoff={}", module, cutoff);
        queryService.deleteModuleBefore(module, cutoff);
        return "ok";
    }
}
