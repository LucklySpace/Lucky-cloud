package com.xy.lucky.message.controller;

import com.xy.lucky.message.domain.dto.ChatDto;
import com.xy.lucky.message.domain.dto.MessageAckDto;
import com.xy.lucky.message.domain.dto.MessageReplayDto;
import com.xy.lucky.message.domain.dto.validation.ValidationGroups;
import com.xy.lucky.message.service.MessageService;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImSingleMessagePo;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/message", "/api/{version}/message"})
@Tag(name = "message", description = "消息")
public class MessageController {

    @Resource
    private MessageService messageService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @PostMapping("/single")
    @Operation(summary = "发送单聊消息", description = "发送私聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IMSingleMessage.class)))
    })
    @Parameters({
            @Parameter(name = "singleMessageDto", description = "消息内容", required = true, in = ParameterIn.DEFAULT)
    })
    public IMSingleMessage sendSingleMessage(@Valid @RequestBody IMSingleMessage singleMessageDto) {
        return messageService.sendSingleMessage(singleMessageDto);
    }

    @PostMapping("/group")
    @Operation(summary = "发送群聊消息", description = "发送群组消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "发送成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = IMGroupMessage.class)))
    })
    @Parameters({
            @Parameter(name = "groupMessageDto", description = "消息内容", required = true, in = ParameterIn.DEFAULT)
    })
    public IMGroupMessage sendGroupMessage(@Valid @RequestBody IMGroupMessage groupMessageDto) {
        return  messageService.sendGroupMessage(groupMessageDto);
    }

    @PostMapping("/media/video")
    @Operation(summary = "发送视频消息", description = "发送视频通话请求")
    @Parameters({
            @Parameter(name = "videoMessageDto", description = "视频消息", required = true, in = ParameterIn.DEFAULT)
    })
    public void sendVideoMessage(@Valid @RequestBody IMVideoMessage videoMessageDto) {
         messageService.sendVideoMessage(videoMessageDto);
    }

    @PostMapping("/recall")
    @Operation(summary = "撤回消息", description = "撤回已发送的消息（2分钟内）")
    @Parameters({
            @Parameter(name = "messageAction", description = "撤回请求", required = true, in = ParameterIn.DEFAULT)
    })
    public void recallMessage(@Valid @RequestBody IMessageAction messageAction) {
         messageService.recallMessage(messageAction);
    }

    @PostMapping("/ack")
    @Operation(summary = "消息确认", description = "客户端确认消息送达")
    @Parameters({
            @Parameter(name = "ackDto", description = "确认请求", required = true, in = ParameterIn.DEFAULT)
    })
    public void acknowledge(@Valid @RequestBody MessageAckDto ackDto) {
        messageService.acknowledge(ackDto.getMessageId(), ackDto.getUserId());
    }

    @PostMapping("/offline/replay")
    @Operation(summary = "离线补发", description = "触发用户离线消息补发")
    @Parameters({
            @Parameter(name = "replayDto", description = "重放请求", required = true, in = ParameterIn.DEFAULT)
    })
    public void replayOfflineMessages(@Valid @RequestBody MessageReplayDto replayDto) {
       messageService.replayOfflineMessages(replayDto.getUserId());
    }

    @PostMapping("/single/list")
    @Operation(summary = "拉取私聊消息列表", description = "根据序列号增量拉取私聊消息")
    @Parameters({
            @Parameter(name = "chatDto", description = "查询条件", required = true, in = ParameterIn.DEFAULT)
    })
    public List<ImSingleMessagePo> singleList(@RequestBody @Validated(ValidationGroups.Query.class) ChatDto chatDto) {
        return messageService.singleList(chatDto);
    }

    @PostMapping("/group/list")
    @Operation(summary = "拉取群聊消息列表", description = "根据序列号增量拉取群聊消息")
    @Parameters({
            @Parameter(name = "chatDto", description = "查询条件", required = true, in = ParameterIn.DEFAULT)
    })
    public List<ImGroupMessagePo> groupList(@RequestBody @Validated(ValidationGroups.Query.class) ChatDto chatDto) {
        return messageService.groupList(chatDto);
    }


}
