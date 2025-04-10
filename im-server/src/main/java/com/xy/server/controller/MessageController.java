package com.xy.server.controller;


import com.xy.server.domain.dto.ChatDto;
import com.xy.server.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/message")
@Tag(name = "message", description = "消息")
public class MessageController {

    @Resource
    private MessageService messageService;


    @PostMapping("/list")
    @Operation(summary = "拉取消息", tags = {"message"}, description = "请使用此接口拉取单聊群聊消息")
    @Parameters({
            @Parameter(name = "chatDto", description = "会话对象", required = true, in = ParameterIn.QUERY)
    })
    public Map<Integer, Object> list(@RequestBody ChatDto chatDto) {
        return messageService.list(chatDto);
    }

    @PostMapping("/singleCheck")
    @Operation(summary = "单聊消息检查", tags = {"message"}, description = "请使用此接口检查单聊消息")
    @Parameters({
            @Parameter(name = "chatDto", description = "会话对象", required = true, in = ParameterIn.QUERY)
    })
    public List singleCheck(@RequestBody ChatDto chatDto) {
        return messageService.singleCheck(chatDto);
    }


}
