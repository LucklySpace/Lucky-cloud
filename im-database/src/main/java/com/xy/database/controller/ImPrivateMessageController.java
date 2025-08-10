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
    public List<ImPrivateMessagePo> list(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imPrivateMessageService.list(userId, sequence);
    }

    /**
     * 获取最后一条消息
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @return 私聊消息
     */
    @GetMapping("/last")
    public ImPrivateMessagePo last(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId) {
        return imPrivateMessageService.last(fromId, toId);
    }

    /**
     * 查询阅读状态
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @param code   阅读状态码
     * @return 未/已读消息数
     */
    @GetMapping("/selectReadStatus")
    public Integer selectReadStatus(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId, @RequestParam("code") Integer code) {
        return imPrivateMessageService.selectReadStatus(fromId, toId, code);
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
