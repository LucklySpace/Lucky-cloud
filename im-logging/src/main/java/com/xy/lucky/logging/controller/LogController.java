package com.xy.lucky.logging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.domain.LogLevel;
import com.xy.lucky.logging.domain.vo.LogRecordVo;
import com.xy.lucky.logging.mapper.LogMapper;
import com.xy.lucky.logging.service.LogAnalysisService;
import com.xy.lucky.logging.service.LogIngestService;
import com.xy.lucky.logging.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
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
    private final ObjectMapper objectMapper;
    private final LogMapper converter;

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
    public Mono<String> ingest(@RequestBody @Valid LogRecordVo record) {
        if (log.isDebugEnabled()) {
            log.debug("接收日志采集请求: module={} level={}", record.getModule(), record.getLevel());
        }
        return Mono.fromRunnable(() -> ingestService.ingest(record))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(record.getId());
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
    public Mono<Integer> ingestBatch(@RequestBody @Valid List<LogRecordVo> records) {
        int size = records != null ? records.size() : 0;
        if (log.isDebugEnabled()) {
            log.debug("接收批量日志采集请求: count={}", size);
        }
        return Mono.fromRunnable(() -> ingestService.ingestBatch(records))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(size);
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
    public Mono<Page<LogRecordVo>> query(
            @Parameter(description = "模块名（不传则不过滤）", example = "im-server") @RequestParam(name = "module", required = false) String module,
            @Parameter(description = "开始时间（ISO-8601，不传默认从1970开始）", example = "2025-12-24T00:00:00Z")
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @Parameter(description = "结束时间（ISO-8601，不传默认当前时间）", example = "2025-12-24T23:59:59Z")
            @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @Parameter(description = "日志级别（不传则不过滤）", example = "ERROR") @RequestParam(name = "level", required = false) LogLevel level,
            @Parameter(description = "服务名（不传则不过滤）", example = "order-service") @RequestParam(name = "service", required = false) String service,
            @Parameter(description = "环境（不传则不过滤）", example = "prod") @RequestParam(name = "env", required = false) String env,
            @Parameter(description = "页码（从0开始）", example = "0") @RequestParam(name = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20") @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "关键字（对message做包含匹配）", example = "timeout") @RequestParam(name = "keyword", required = false) String keyword) {
        if (log.isDebugEnabled()) {
            log.debug("查询日志请求: module={} service={} env={} level={} keyword={}", module, service, env, level, keyword);
        }
        return Mono.fromCallable(() -> queryService.query(module, start, end, level, service, env, page, size, keyword))
                .subscribeOn(Schedulers.boundedElastic());
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
    public Mono<Map<String, Object>> overview() {
        return Mono.fromCallable(analysisService::overview).subscribeOn(Schedulers.boundedElastic());
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
    public Mono<Map<String, Long>> hourly(
            @Parameter(description = "日志级别") @RequestParam(name = "level", defaultValue = "ERROR") String level,
            @Parameter(description = "最近小时数") @RequestParam(name = "hours", defaultValue = "24") int hours) {
        return Mono.fromCallable(() -> analysisService.hourlySeries(level, hours))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/stats/histogram")
    public Mono<Map<String, Long>> histogram(
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(name = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(name = "level", required = false) LogLevel level,
            @RequestParam(name = "service", required = false) String service,
            @RequestParam(name = "env", required = false) String env,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "interval", defaultValue = "hour") String interval) {
        return Mono.fromCallable(() -> queryService.histogram(module, start, end, level, service, env, keyword, interval))
                .subscribeOn(Schedulers.boundedElastic());
    }


    @Operation(summary = "元数据", description = "获取元数据，包括模块、服务、环境、地址")
    @GetMapping("/meta/services")
    public Mono<List<String>> metaServices(@RequestParam(name = "env", required = false) String env) {
        return Mono.fromCallable(() -> queryService.listServices(env)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/aggs/top/services")
    public Mono<List<Map<String, Object>>> aggsTopServices(
            @RequestParam(name = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(name = "end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return Mono.fromCallable(() -> queryService.topServices(start, end, limit)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/aggs/top/addresses")
    public Mono<List<Map<String, Object>>> aggsTopAddresses(
            @RequestParam(name = "start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(name = "end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        return Mono.fromCallable(() -> queryService.topAddresses(start, end, limit)).subscribeOn(Schedulers.boundedElastic());
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
    public Mono<String> deleteBefore(@Parameter(description = "截止时间") @RequestParam(name = "cutoff") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoff) {
        log.warn("执行日志清理: cutoff={}", cutoff);
        return Mono.fromRunnable(() -> queryService.deleteBefore(cutoff))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn("ok");
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
            @Parameter(description = "模块名") @PathVariable("module") String module,
            @Parameter(description = "截止时间") @RequestParam(name = "cutoff") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoff) {
        log.warn("执行模块日志清理: module={} cutoff={}", module, cutoff);
        queryService.deleteModuleBefore(module, cutoff);
        return "ok";
    }
}
