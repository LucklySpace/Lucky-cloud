package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImPrivateMessageService;
import com.xy.domain.po.ImPrivateMessagePo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/private/message")
@Tag(name = "ImPrivate", description = "私聊消息数据库接口")
@RequiredArgsConstructor
public class ImPrivateMessageController {

    private final ImPrivateMessageService imPrivateMessageService;

    /**
     * 获取用户私聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/list")
    List<ImPrivateMessagePo> list(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imPrivateMessageService.list(userId, sequence);
    }


    /**
     * 插入私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImPrivateMessagePo messagePo) {
        return imPrivateMessageService.save(messagePo);
    }

    /**
     * 更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/updateById")
    public Boolean updateById(@RequestBody ImPrivateMessagePo messagePo) {
        return imPrivateMessageService.updateById(messagePo);
    }

    /**
     * 删除私聊消息
     *
     * @param messageId 私聊消息id
     */
    @DeleteMapping("/{messageId}")
    public Boolean deleteById(@PathVariable String messageId) {
        return imPrivateMessageService.removeById(messageId);
    }

}
