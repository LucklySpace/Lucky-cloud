package com.xy.lucky.database.web.controller;


import com.xy.lucky.database.web.service.IMOutboxService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/outbox")
@Tag(name = "IMOutbox", description = "用户会话数据库接口")
@Validated
public class IMOutboxController {

    @Resource
    private IMOutboxService imOutboxService;

    /**
     * 查询消息列表
     *
     * @return 消息列表
     */
    @GetMapping("/selectList")
    @Operation(summary = "查询Outbox消息列表", description = "返回所有待投递/已投递消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMOutboxPo.class)))
    })
    public Mono<List<IMOutboxPo>> listOutboxMessages() {
        return Mono.fromCallable(imOutboxService::queryList).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取单个
     *
     * @param id
     * @return
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取Outbox消息", description = "返回指定ID的Outbox消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMOutboxPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<IMOutboxPo> getOutboxMessageById(@RequestParam("id") @NotNull @Positive Long id) {
        return Mono.fromCallable(() -> imOutboxService.queryOne(id)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 保存或更新
     *
     * @param outboxPo
     * @return
     */
    @PostMapping("/insert")
    @Operation(summary = "创建Outbox消息", description = "新增一条待投递消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createOutboxMessage(@RequestBody @Valid IMOutboxPo outboxPo) {
        return Mono.fromCallable(() -> imOutboxService.creat(outboxPo)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 批量插入消息
     *
     * @param outboxPoList 消息列表
     * @return 是否插入成功
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建Outbox消息", description = "批量新增待投递消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createOutboxMessagesBatch(@RequestBody @NotEmpty List<@Valid IMOutboxPo> outboxPoList) {
        return Mono.fromCallable(() -> imOutboxService.creatBatch(outboxPoList)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新消息
     *
     * @param outboxPo 消息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    @Operation(summary = "更新Outbox消息", description = "根据ID更新Outbox消息内容或状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateOutboxMessage(@RequestBody @Valid IMOutboxPo outboxPo) {
        return Mono.fromCallable(() -> imOutboxService.modify(outboxPo)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除Outbox消息", description = "根据ID删除Outbox消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteOutboxMessageById(@RequestParam("id") @NotNull @Positive Long id) {
        return Mono.fromCallable(() -> imOutboxService.removeOne(id)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 批量获取待发送的消息
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 消息列表
     */
    @GetMapping("/selectListByStatus")
    @Operation(summary = "按状态查询Outbox消息", description = "根据状态与数量限制查询Outbox消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMOutboxPo.class)))
    })
    public Mono<List<IMOutboxPo>> listOutboxMessagesByStatus(@RequestParam("status") @NotBlank @Pattern(regexp = "PENDING|SENT|DELIVERED|FAILED") String status,
                                                             @RequestParam("limit") @NotNull @Positive @Max(1000) Integer limit) {
        return Mono.fromCallable(() -> imOutboxService.queryByStatus(status, limit)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新消息状态
     *
     * @param id       消息ID
     * @param status   状态
     * @param attempts 尝试次数
     * @return 是否更新成功
     */
    @PutMapping("/updateStatus")
    @Operation(summary = "更新Outbox消息状态", description = "更新消息投递状态与尝试次数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateOutboxStatus(@RequestParam("id") @NotNull @Positive Long id,
                                            @RequestParam("status") @NotBlank @Pattern(regexp = "PENDING|SENT|DELIVERED|FAILED") String status,
                                            @RequestParam("attempts") @NotNull @PositiveOrZero @Max(1000) Integer attempts) {
        return Mono.fromCallable(() -> imOutboxService.modifyStatus(id, status, attempts)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新消息为发送失败
     *
     * @param id        消息ID
     * @param lastError 错误信息
     * @param attempts  尝试次数
     * @return 是否更新成功
     */
    @PutMapping("/markAsFailed")
    @Operation(summary = "标记Outbox消息为失败", description = "记录错误信息并更新尝试次数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> markOutboxMessageFailed(@RequestParam("id") @NotNull @Positive Long id,
                                                 @RequestParam("lastError") @NotBlank @Size(max = 1024) String lastError,
                                                 @RequestParam("attempts") @NotNull @PositiveOrZero @Max(1000) Integer attempts) {
        return Mono.fromCallable(() -> imOutboxService.modifyToFailed(id, lastError, attempts)).subscribeOn(Schedulers.boundedElastic());
    }
}
