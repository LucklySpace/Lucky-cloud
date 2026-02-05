package com.xy.lucky.database.web.controller;


import com.xy.lucky.database.web.security.SecurityInner;
import com.xy.lucky.database.web.service.ImChatService;
import com.xy.lucky.domain.po.ImChatPo;
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
@RequestMapping("/api/{version}/database/chat")
@Tag(name = "ImChat", description = "用户会话数据库接口")
@Validated
public class ImChatController {

    @Resource
    private ImChatService imChatService;

    @GetMapping("/selectList")
    @Operation(summary = "查询用户会话列表", description = "根据用户ID与序列查询会话列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImChatPo.class)))
    })
    public Mono<List<ImChatPo>> listChats(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                          @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return Mono.fromCallable(() -> imChatService.queryList(ownerId, sequence)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/selectOne")
    @Operation(summary = "获取单个会话信息", description = "根据ownerId、toId与可选的chatType查询单个会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImChatPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImChatPo> getChat(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                  @RequestParam("toId") @NotBlank @Size(max = 64) String toId,
                                  @RequestParam(value = "chatType", required = false) @Min(0) Integer chatType) {
        return Mono.fromCallable(() -> imChatService.queryOne(ownerId, toId, chatType)).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/insert")
    @Operation(summary = "创建会话", description = "新增会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createChat(@RequestBody @Valid ImChatPo chatPo) {
        return Mono.fromCallable(() -> imChatService.creat(chatPo)).subscribeOn(Schedulers.boundedElastic());
    }

    @SecurityInner
    @PutMapping("/update")
    @Operation(summary = "更新会话", description = "根据ID更新会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateChat(@RequestBody @Valid ImChatPo chatPo) {
        return Mono.fromCallable(() -> imChatService.modify(chatPo)).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除会话", description = "根据ID删除会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteChatById(@RequestParam("id") @NotBlank @Size(max = 64) String id) {
        return Mono.fromCallable(() -> imChatService.removeOne(id)).subscribeOn(Schedulers.boundedElastic());
    }

}
