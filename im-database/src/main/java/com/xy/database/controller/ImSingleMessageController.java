package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImSingleMessageService;
import com.xy.domain.po.ImSingleMessagePo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/single/message")
@Tag(name = "ImPrivate", description = "私聊消息数据库接口")
@RequiredArgsConstructor
public class ImSingleMessageController {

    private final ImSingleMessageService imSingleMessageService;


    /**
     * 根据id查询私聊消息
     *
     * @param messageId 私聊消息id
     * @return 私聊消息
     */
    @GetMapping("/getById")
    public ImSingleMessagePo getById(@RequestParam("messageId") String messageId) {
        return imSingleMessageService.getById(messageId);
    }

    /**
     * 获取用户私聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/list")
    public List<ImSingleMessagePo> list(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imSingleMessageService.list(userId, sequence);
    }

    /**
     * 获取最后一条消息
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @return 私聊消息
     */
    @GetMapping("/last")
    public ImSingleMessagePo last(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId) {
        return imSingleMessageService.last(fromId, toId);
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
        return imSingleMessageService.selectReadStatus(fromId, toId, code);
    }

    /**
     * 保存或更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/saveOrUpdate")
    public Boolean saveOrUpdate(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.saveOrUpdate(messagePo);
    }


    /**
     * 删除私聊消息
     *
     * @param messageId 私聊消息id
     */
    @DeleteMapping("/{messageId}")
    public Boolean deleteById(@PathVariable String messageId) {
        return imSingleMessageService.removeById(messageId);
    }

}
