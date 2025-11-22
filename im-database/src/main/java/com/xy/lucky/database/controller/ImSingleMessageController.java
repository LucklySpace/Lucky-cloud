package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImSingleMessageService;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/single/message")
@Tag(name = "ImPrivate", description = "私聊消息数据库接口")
public class ImSingleMessageController {

    @Resource
    private ImSingleMessageService imSingleMessageService;


    /**
     * 根据id查询私聊消息
     *
     * @param messageId 私聊消息id
     * @return 私聊消息
     */
    @GetMapping("/selectOne")
    public ImSingleMessagePo selectOne(@RequestParam("messageId") String messageId) {
        return imSingleMessageService.selectOne(messageId);
    }

    /**
     * 获取用户私聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/selectList")
    public List<ImSingleMessagePo> selectList(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imSingleMessageService.selectList(userId, sequence);
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
     * 插入私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.insert(messagePo);
    }


    /**
     * 删除私聊消息
     *
     * @param messageId 私聊消息id
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("messageId") String messageId) {
        return imSingleMessageService.deleteById(messageId);
    }
    
    /**
     * 批量插入私聊消息
     *
     * @param messagePoList 私聊消息列表
     */
    @PostMapping("/batchInsert")
    public Boolean batchInsert(@RequestBody List<ImSingleMessagePo> messagePoList) {
        return imSingleMessageService.batchInsert(messagePoList);
    }

    /**
     * 插入或更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/saveOrUpdate")
    public Boolean saveOrUpdate(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.saveOrUpdate(messagePo);
    }
    
    /**
     * 更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.update(messagePo);
    }

}