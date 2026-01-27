package com.xy.lucky.server.controller;


import com.xy.lucky.server.domain.dto.ChatDto;
import com.xy.lucky.server.domain.vo.ChatVo;
import com.xy.lucky.server.service.ChatService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
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
    @Operation(summary = "查询用户会话", tags = {"chat"}, description = "请使用此接口查找用户会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatVo.class)))
    })
    @Parameters({
            @Parameter(name = "chatSetDto", description = "用户会话信息", required = true, in = ParameterIn.QUERY)
    })
    public Mono<List<ChatVo>> list(@RequestBody ChatDto chatDto) {
        return Mono.fromCallable(() -> chatService.list(chatDto))
                .subscribeOn(getScheduler());
    }


    @PostMapping("/read")
    @Operation(summary = "用户会话已读", tags = {"chat"}, description = "请使用此接口设置会话已读")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json"))
    })
    @Parameters({
            @Parameter(name = "chatSetDto", description = "用户会话已读", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> read(@RequestBody ChatDto chatDto) {
        return Mono.fromRunnable(() -> chatService.read(chatDto))
                .subscribeOn(getScheduler())
                .then();
    }

    @GetMapping("/one")
    @Operation(summary = "查询用户会话", tags = {"chat"}, description = "请使用此接口获取用户会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatVo.class)))
    })
    @Parameters({
            @Parameter(name = "ownerId", description = "对象", required = true, in = ParameterIn.DEFAULT),
            @Parameter(name = "toId", description = "对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<ChatVo> one(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId) {
        return Mono.fromCallable(() -> chatService.one(ownerId, toId))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/create")
    @Operation(summary = "用户单向创建会话", tags = {"chat"}, description = "请使用此接口创建会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatVo.class)))
    })
    @Parameters({
            @Parameter(name = "ChatSetDto", description = "用户单向创建会话", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<ChatVo> create(@RequestBody ChatDto ChatDto) {
        return Mono.fromCallable(() -> chatService.create(ChatDto))
                .subscribeOn(getScheduler());
    }

}
