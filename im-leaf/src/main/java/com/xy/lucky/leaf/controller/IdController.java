package com.xy.lucky.leaf.controller;

import com.xy.lucky.core.model.IMetaId;
import com.xy.lucky.leaf.service.IdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * ID生成接口控制器
 * 提供多种ID生成策略的REST API接口
 */
@Tag(name = "ID Generator", description = "ID生成服务接口")
@RestController
@Validated
@RequestMapping({"/api/generator", "/api/{version}/generator"})
public class IdController {

    @Resource
    private IdService idService;

    /**
     * 根据类型和业务标识异步生成单个ID（响应式）
     *
     * @param type 策略类型：snowflake | redis | uid | uuid
     * @param key  业务标识
     * @return ID对象的Mono流
     */
    @Operation(summary = "生成单个ID", description = "根据指定策略和业务标识生成单个ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMetaId.class)))
    })
    @GetMapping("/id")
    public Mono<IMetaId> generateId(
            @Parameter(description = "策略类型") @RequestParam("type") String type,
            @Parameter(description = "业务标识") @RequestParam("key") String key) {
        return Mono.fromCallable(() -> idService.generateId(type, key));
    }

    /**
     * 批量获取ID（响应式）
     *
     * @param type  策略类型：snowflake | redis | uid | uuid
     * @param key   业务标识
     * @param count 获取数量
     * @return List ids 的Mono流
     */
    @Operation(summary = "批量生成ID", description = "根据指定策略和业务标识批量生成ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMetaId.class)))
    })
    @GetMapping("/ids")
    public Mono<List<IMetaId>> generateBatchIds(
            @Parameter(description = "策略类型") @RequestParam("type") String type,
            @Parameter(description = "业务标识") @RequestParam("key") String key,
            @Parameter(description = "生成数量") @RequestParam("count") @Min(1) @Max(1000) Integer count) {
        return Mono.fromCallable(() -> idService.generateIds(type, key, count));
    }
}
