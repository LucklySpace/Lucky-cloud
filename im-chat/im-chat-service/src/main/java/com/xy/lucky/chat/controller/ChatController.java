package com.xy.lucky.chat.controller;

import com.xy.lucky.chat.domain.dto.ChatDto;
import com.xy.lucky.chat.domain.dto.validation.ValidationGroups;
import com.xy.lucky.chat.domain.vo.ChatVo;
import com.xy.lucky.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/chat", "/api/{version}/chat"})
@Tag(name = "chat", description = "用户会话")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @PostMapping("/list")
    @Operation(summary = "查询会话列表", description = "查询用户的会话列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatVo.class)))
    })
    @Parameters({
            @Parameter(name = "chatDto", description = "查询条件", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<List<ChatVo>> list(@RequestBody @Validated(ValidationGroups.Query.class) ChatDto chatDto) {
        return Mono.fromCallable(() -> chatService.list(chatDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/read")
    @Operation(summary = "标记已读", description = "标记会话消息为已读")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    @Parameters({
            @Parameter(name = "chatDto", description = "会话信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> read(@RequestBody @Validated(ValidationGroups.Create.class) ChatDto chatDto) {
        return Mono.fromRunnable(() -> chatService.read(chatDto))
                .subscribeOn(getScheduler())
                .then();
    }

    @GetMapping("/one")
    @Operation(summary = "查询单个会话", description = "根据所有者ID和目标ID查询会话详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatVo.class)))
    })
    @Parameters({
            @Parameter(name = "ownerId", description = "所有者ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "toId", description = "目标ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<ChatVo> one(
            @RequestParam("ownerId") @NotBlank(message = "所有者ID不能为空") String ownerId,
            @RequestParam("toId") @NotBlank(message = "目标ID不能为空") String toId) {
        return Mono.fromCallable(() -> chatService.one(ownerId, toId))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/create")
    @Operation(summary = "创建会话", description = "创建单向会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatVo.class)))
    })
    @Parameters({
            @Parameter(name = "chatDto", description = "会话信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<ChatVo> create(@RequestBody @Validated(ValidationGroups.Create.class) ChatDto chatDto) {
        return Mono.fromCallable(() -> chatService.create(chatDto))
                .subscribeOn(getScheduler());
    }
}
