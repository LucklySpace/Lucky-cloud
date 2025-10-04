package com.xy.server.controller;


import com.xy.domain.dto.ChatDto;
import com.xy.domain.vo.ChatVo;
import com.xy.general.response.domain.Result;
import com.xy.server.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/chat")
@Tag(name = "chat", description = "用户会话")
public class ChatController {

    @Resource
    private ChatService chatService;

    @PostMapping("/list")
    @Operation(summary = "查询用户会话", tags = {"chat"}, description = "请使用此接口查找用户会话")
    @Parameters({
            @Parameter(name = "chatSetDto", description = "用户会话信息", required = true, in = ParameterIn.QUERY)
    })
    public List<ChatVo> list(@RequestBody ChatDto chatDto) {
        return chatService.list(chatDto);
    }


    @PostMapping("/read")
    @Operation(summary = "用户会话已读", tags = {"chat"}, description = "请使用此接口设置会话已读")
    @Parameters({
            @Parameter(name = "chatSetDto", description = "用户会话已读", required = true, in = ParameterIn.DEFAULT)
    })
    public Result read(@RequestBody ChatDto chatDto) {
        return chatService.read(chatDto);
    }

    @GetMapping("/one")
    @Operation(summary = "查询用户会话", tags = {"chat"}, description = "请使用此接口获取用户会话")
    @Parameters({
            @Parameter(name = "ownerId", description = "对象", required = true, in = ParameterIn.DEFAULT),
            @Parameter(name = "toId", description = "对象", required = true, in = ParameterIn.DEFAULT)
    })
    public ChatVo one(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId) {
        return chatService.one(ownerId, toId);
    }

    @PostMapping("/create")
    @Operation(summary = "用户单向创建会话", tags = {"chat"}, description = "请使用此接口创建会话")
    @Parameters({
            @Parameter(name = "ChatSetDto", description = "用户单向创建会话", required = true, in = ParameterIn.DEFAULT)
    })
    public ChatVo create(@RequestBody ChatDto ChatDto) {
        return chatService.create(ChatDto);
    }

}
