package com.xy.lucky.server.controller;

import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.server.service.MessageService;
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
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping({"/api/message", "/api/{version}/message"})
@Tag(name = "message", description = "消息")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    /**
     * 获取虚拟线程调度器
     */
    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @PostMapping("/single")
    @Operation(summary = "单聊发送消息", tags = {"single"}, description = "请使用此接口发送单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMSingleMessage.class)))
    })
    @Parameters({
            @Parameter(name = "singleMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<IMSingleMessage> sendSingleMessage(@Valid @RequestBody IMSingleMessage singleMessageDto) {
        return Mono.fromCallable(() -> messageService.sendSingleMessage(singleMessageDto))
                .subscribeOn(getScheduler())
                .doOnError(e -> log.error("单聊消息发送失败: from={}, to={}, error={}",
                        singleMessageDto.getFromId(), singleMessageDto.getToId(), e.getMessage()));
    }

    @PostMapping("/group")
    @Operation(summary = "群聊发送消息", tags = {"group"}, description = "请使用此接口发送群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = IMGroupMessage.class)))
    })
    @Parameters({
            @Parameter(name = "groupMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<IMGroupMessage> sendGroupMessage(@Valid @RequestBody IMGroupMessage groupMessageDto) {
        return Mono.fromCallable(() -> messageService.sendGroupMessage(groupMessageDto))
                .subscribeOn(getScheduler())
                .doOnError(e -> log.error("群聊消息发送失败: from={}, group={}, error={}",
                        groupMessageDto.getFromId(), groupMessageDto.getGroupId(), e.getMessage()));
    }

    @PostMapping("/media/video")
    @Operation(summary = "视频发送消息", tags = {"video"}, description = "请使用此接口发送视频消息")
    @Parameters({
            @Parameter(name = "IMVideoMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> sendVideoMessage(@RequestBody IMVideoMessage videoMessageDto) {
        return Mono.fromRunnable(() -> messageService.sendVideoMessage(videoMessageDto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/recall")
    @Operation(summary = "撤回消息", tags = {"message"}, description = "请使用此接口撤回消息")
    @Parameters({
            @Parameter(name = "messageAction", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> recallMessage(@RequestBody IMessageAction messageAction) {
        return Mono.fromRunnable(() -> messageService.recallMessage(messageAction))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/list")
    @Operation(summary = "拉取消息", tags = {"message"}, description = "请使用此接口拉取单聊群聊消息")
    @Parameters({
            @Parameter(name = "chatDto", description = "会话对象", required = true, in = ParameterIn.QUERY)
    })
    public Mono<Map<Integer, Object>> list(@RequestBody ChatDto chatDto) {
        return Mono.fromCallable(() -> messageService.list(chatDto))
                .subscribeOn(getScheduler());
    }
}
