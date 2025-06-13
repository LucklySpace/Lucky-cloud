package com.xy.server.controller;


import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMPrivateMessageDto;
import com.xy.imcore.model.IMVideoMessageDto;
import com.xy.request.annotations.ApiSign;
import com.xy.response.domain.Result;
import com.xy.server.service.MessageService;
import com.xy.version.annotations.Version;
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

@Slf4j
@RestController
@RequestMapping("/api/{version}/message")
@Tag(name = "message", description = "消息")
public class MessageController {

    @Resource
    private MessageService messageService;

    @ApiSign
    @PostMapping("/private")
    @Operation(summary = "单聊发送消息", tags = {"single"}, description = "请使用此接口发送单聊消息")
    @Parameters({
            @Parameter(name = "privateMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result sendPrivateMessage(@Valid @RequestBody IMPrivateMessageDto privateMessageDto) {
        return messageService.sendPrivateMessage(privateMessageDto);
    }

    @PostMapping("/group")
    @Operation(summary = "群聊发送消息", tags = {"group"}, description = "请使用此接口发送群聊消息")
    @Parameters({
            @Parameter(name = "groupMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result sendGroupMessage(@Valid @RequestBody IMGroupMessageDto groupMessageDto) {
        return messageService.sendGroupMessage(groupMessageDto);
    }

    @PostMapping("/media/video")
    @Operation(summary = "视频发送消息", tags = {"video"}, description = "请使用此接口发送视频消息")
    @Parameters({
            @Parameter(name = "IMVideoMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result sendVideoMessage(@RequestBody IMVideoMessageDto videoMessageDto) {
        return messageService.sendVideoMessage(videoMessageDto);
    }


//    @PostMapping("/list")
//    @Operation(summary = "拉取消息", tags = {"message"}, description = "请使用此接口拉取单聊群聊消息")
//    @Parameters({
//            @Parameter(name = "chatDto", description = "会话对象", required = true, in = ParameterIn.QUERY)
//    })
//    public Map<Integer, Object> list(@RequestBody ChatDto chatDto) {
//        return messageService.list(chatDto);
//    }
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
