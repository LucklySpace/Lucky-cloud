package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImSingleMessageReactiveService;
import com.xy.lucky.domain.po.ImSingleMessagePo;
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
@RequestMapping("/api/{version}/database/single/message")
@Tag(name = "ImPrivate", description = "私聊消息数据库接口(WebFlux-R2DBC)")
@Validated
public class ImSingleMessageReactiveController {

    @Resource
    private ImSingleMessageReactiveService imSingleMessageService;

    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取私聊消息", description = "返回指定消息ID的单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImSingleMessagePo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImSingleMessagePo> getSingleMessageById(@RequestParam("messageId") @NotBlank @Size(max = 64) String messageId) {
        return imSingleMessageService.queryOne(messageId);
    }

    @GetMapping("/selectList")
    @Operation(summary = "获取用户单聊消息列表", description = "按用户ID与序列获取单聊消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImSingleMessagePo.class)))
    })
    public Mono<List<ImSingleMessagePo>> listSingleMessages(@RequestParam("userId") @NotBlank @Size(max = 64) String userId, @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return imSingleMessageService.queryList(userId, sequence).collectList();
    }

    @GetMapping("/last")
    @Operation(summary = "获取最后一条单聊消息", description = "按双方ID查询最新单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImSingleMessagePo.class)))
    })
    public Mono<ImSingleMessagePo> getLastSingleMessage(@RequestParam("fromId") @NotBlank @Size(max = 64) String fromId, @RequestParam("toId") @NotBlank @Size(max = 64) String toId) {
        return imSingleMessageService.queryLast(fromId, toId);
    }

    @GetMapping("/selectReadStatus")
    @Operation(summary = "查询单聊消息阅读状态", description = "按双方ID与状态码统计消息阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<Integer> getSingleMessageReadStatus(@RequestParam("fromId") @NotBlank @Size(max = 64) String fromId, @RequestParam("toId") @NotBlank @Size(max = 64) String toId, @RequestParam("code") @NotNull @PositiveOrZero Integer code) {
        return imSingleMessageService.queryReadStatus(fromId, toId, code);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建单聊消息", description = "新增单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createSingleMessage(@RequestBody @Valid ImSingleMessagePo messagePo) {
        return imSingleMessageService.create(messagePo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除单聊消息", description = "根据ID删除单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteSingleMessageById(@RequestParam("messageId") @NotBlank @Size(max = 64) String messageId) {
        return imSingleMessageService.removeOne(messageId);
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建单聊消息", description = "批量新增单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createSingleMessagesBatch(@RequestBody @NotEmpty List<@Valid ImSingleMessagePo> messagePoList) {
        return imSingleMessageService.createBatch(messagePoList);
    }

    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增或更新单聊消息", description = "存在则更新，不存在则新增")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Boolean> upsertSingleMessage(@RequestBody @Valid ImSingleMessagePo messagePo) {
        return imSingleMessageService.saveOrUpdate(messagePo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新单聊消息", description = "根据ID更新单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateSingleMessage(@RequestBody @Valid ImSingleMessagePo messagePo) {
        return imSingleMessageService.modify(messagePo);
    }
}
