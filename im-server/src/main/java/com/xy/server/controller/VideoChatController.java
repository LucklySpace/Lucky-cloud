package com.xy.server.controller;

import com.xy.imcore.model.IMVideoMessageDto;
import com.xy.server.service.VideoChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/video")
@Tag(name = "video", description = "视频")
public class VideoChatController {


    @Resource
    private VideoChatService videoChatService;


    @PostMapping("/send")
    @Operation(summary = "发送消息", tags = {"video"}, description = "请使用此接口发送视频消息")
    @Parameters({
            @Parameter(name = "IMVideoMessageDto", description = "消息对象", required = true, in = ParameterIn.DEFAULT)
    })
    public void send(@RequestBody IMVideoMessageDto videoMessageDto) {
        videoChatService.send(videoMessageDto);
    }

}
