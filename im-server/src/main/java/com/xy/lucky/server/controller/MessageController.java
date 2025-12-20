package com.xy.lucky.server.controller;


import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.server.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/{version}/message")
@Tag(name = "message", description = "消息")
public class MessageController {

    @Resource
    private MessageService messageService;

    @PostMapping("/single")
    @Operation(summary = "单聊发送消息", tags = {"single"}, description = "请使用此接口发送单聊消息")
    @Parameters({
            @Parameter(name = "singleMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result sendSingleMessage(@Valid @RequestBody IMSingleMessage singleMessageDto) {
        return messageService.sendSingleMessage(singleMessageDto);
    }

    @PostMapping("/group")
    @Operation(summary = "群聊发送消息", tags = {"group"}, description = "请使用此接口发送群聊消息")
    @Parameters({
            @Parameter(name = "groupMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result sendGroupMessage(@Valid @RequestBody IMGroupMessage groupMessageDto) {
        return messageService.sendGroupMessage(groupMessageDto);
    }

    @PostMapping("/media/video")
    @Operation(summary = "视频发送消息", tags = {"video"}, description = "请使用此接口发送视频消息")
    @Parameters({
            @Parameter(name = "IMVideoMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result sendVideoMessage(@RequestBody IMVideoMessage videoMessageDto) {
        return messageService.sendVideoMessage(videoMessageDto);
    }


    @PostMapping("/recall")
    @Operation(summary = "撤回消息", tags = {"message"}, description = "请使用此接口撤回消息")
    @Parameters({
            @Parameter(name = "messageAction", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result recallMessage(@RequestBody IMessageAction messageAction) {
        return messageService.recallMessage(messageAction);
    }


    @PostMapping("/list")
    @Operation(summary = "拉取消息", tags = {"message"}, description = "请使用此接口拉取单聊群聊消息")
    @Parameters({
            @Parameter(name = "chatDto", description = "会话对象", required = true, in = ParameterIn.QUERY)
    })
    public Map<Integer, Object> list(@RequestBody ChatDto chatDto) {
        return messageService.list(chatDto);
    }
//
//    @PostMapping("/singleCheck")
//    @Operation(summary = "单聊消息检查", tags = {"message"}, description = "请使用此接口检查单聊消息")
//    @Parameters({
//            @Parameter(name = "chatDto", description = "会话对象", required = true, in = ParameterIn.QUERY)
//    })
//    public List singleCheck(@RequestBody ChatDto chatDto) {
//        return messageService.singleCheck(chatDto);
//    }
//


}
