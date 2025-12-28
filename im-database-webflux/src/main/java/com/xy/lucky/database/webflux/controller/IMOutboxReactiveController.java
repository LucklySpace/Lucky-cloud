package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImOutboxReactiveService;
import com.xy.lucky.domain.po.IMOutboxPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database/outbox")
@Tag(name = "IMOutbox", description = "用户会话数据库接口(WebFlux-R2DBC)")
@Validated
public class IMOutboxReactiveController {

    @Resource
    private ImOutboxReactiveService imOutboxService;

    @GetMapping("/selectList")
    @Operation(summary = "查询Outbox消息列表", description = "返回所有待投递/已投递消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMOutboxPo.class)))
    })
    public Mono<List<IMOutboxPo>> listOutboxMessages() {
        return imOutboxService.queryList().collectList();
    }

    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取Outbox消息", description = "返回指定ID的Outbox消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMOutboxPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<IMOutboxPo> getOutboxMessageById(@RequestParam("id") @NotNull @Positive Long id) {
        return imOutboxService.queryOne(id);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建Outbox消息", description = "新增一条待投递消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createOutboxMessage(@RequestBody @Valid IMOutboxPo outboxPo) {
        return imOutboxService.create(outboxPo);
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建Outbox消息", description = "批量新增待投递消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createOutboxMessagesBatch(@RequestBody @NotEmpty List<@Valid IMOutboxPo> outboxPoList) {
        return imOutboxService.createBatch(outboxPoList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新Outbox消息", description = "根据ID更新Outbox消息内容或状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateOutboxMessage(@RequestBody @Valid IMOutboxPo outboxPo) {
        return imOutboxService.modify(outboxPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除Outbox消息", description = "根据ID删除Outbox消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteOutboxMessageById(@RequestParam("id") @NotNull @Positive Long id) {
        return imOutboxService.removeOne(id);
    }

    @GetMapping("/selectListByStatus")
    @Operation(summary = "按状态查询Outbox消息", description = "根据状态与数量限制查询Outbox消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMOutboxPo.class)))
    })
    public Mono<List<IMOutboxPo>> listOutboxMessagesByStatus(@RequestParam("status") @NotBlank @Pattern(regexp = "PENDING|SENT|DELIVERED|FAILED") String status,
                                                             @RequestParam("limit") @NotNull @Positive @Max(1000) Integer limit) {
        return imOutboxService.queryByStatus(status, limit).collectList();
    }

    @PutMapping("/updateStatus")
    @Operation(summary = "更新Outbox消息状态", description = "更新消息投递状态与尝试次数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateOutboxStatus(@RequestParam("id") @NotNull @Positive Long id,
                                            @RequestParam("status") @NotBlank @Pattern(regexp = "PENDING|SENT|DELIVERED|FAILED") String status,
                                            @RequestParam("attempts") @NotNull @PositiveOrZero @Max(1000) Integer attempts) {
        return imOutboxService.modifyStatus(id, status, attempts);
    }

    @PutMapping("/markAsFailed")
    @Operation(summary = "标记Outbox消息为失败", description = "记录错误信息并更新尝试次数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> markOutboxMessageFailed(@RequestParam("id") @NotNull @Positive Long id,
                                                 @RequestParam("lastError") @NotBlank @Size(max = 1024) String lastError,
                                                 @RequestParam("attempts") @NotNull @PositiveOrZero @Max(1000) Integer attempts) {
        return imOutboxService.modifyToFailed(id, lastError, attempts);
    }
}
