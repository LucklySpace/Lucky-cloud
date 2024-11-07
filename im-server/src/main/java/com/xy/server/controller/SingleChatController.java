package com.xy.server.controller;


import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.server.response.Result;
import com.xy.server.service.SingleChatService;
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

/**
 * 单聊
 */
@Slf4j
@RestController
@RequestMapping("/message/single")
@Tag(name = "single", description = "单聊")
public class SingleChatController {


    @Resource
    private SingleChatService singleChatService;


    @PostMapping("/send")
    @Operation(summary = "发送消息", tags = {"single"}, description = "请使用此接口发送单聊消息")
    @Parameters({
            @Parameter(name = "singleMessageDto", description = "消息对象", required = true, in = ParameterIn.QUERY)
    })
    public Result send(@RequestBody IMSingleMessageDto singleMessageDto) {
        return singleChatService.send(singleMessageDto);
    }

}
