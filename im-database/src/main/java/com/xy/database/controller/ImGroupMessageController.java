package com.xy.database.controller;

import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImGroupMessageService;
import com.xy.database.service.ImGroupMessageStatusService;
import com.xy.domain.po.ImGroupMessagePo;
import com.xy.domain.po.ImGroupMessageStatusPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/group/message")
@Tag(name = "ImGroup", description = "群聊消息数据库接口")
public class ImGroupMessageController {

    @Resource
    private ImGroupMessageService imGroupMessageService;

    @Resource
    private ImGroupMessageStatusService imGroupMessageStatusService;

    /**
     * 获取群聊消息
     *
     * @param messageId 消息id
     * @return 群聊消息
     */
    @GetMapping("/selectOne")
    public ImGroupMessagePo selectOne(@RequestParam("messageId") String messageId) {
        return imGroupMessageService.selectOne(messageId);
    }

    /**
     * 获取用户群聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/selectList")
    public List<ImGroupMessagePo> selectList(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imGroupMessageService.selectList(userId, sequence);
    }

    /**
     * 插入群聊消息
     *
     * @param messagePo 群聊消息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImGroupMessagePo messagePo) {
        return imGroupMessageService.insert(messagePo);
    }

    /**
     * 批量插入群聊消息
     *
     * @param messagePoList 群聊消息列表
     */
    @PostMapping("/batchInsert")
    public Boolean batchInsert(@RequestBody List<ImGroupMessageStatusPo> messagePoList) {
        return imGroupMessageService.batchInsert(messagePoList);
    }

    /**
     * 更新群聊消息
     *
     * @param messagePo 群聊消息
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImGroupMessagePo messagePo) {
        return imGroupMessageService.update(messagePo);
    }

    /**
     * 删除群聊消息
     *
     * @param messageId 消息ID
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("messageId") String messageId) {
        return imGroupMessageService.deleteById(messageId);
    }

    /**
     * 获取最后一条消息
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @return 私聊消息
     */
    @GetMapping("/last")
    public ImGroupMessagePo last(@RequestParam("groupId") String groupId, @RequestParam("userId") String userId) {
        return imGroupMessageService.last(groupId, userId);
    }

    /**
     * 查询阅读状态
     *
     * @param groupId 群id
     * @param toId    接受者id
     * @param code    阅读状态码
     * @return 未/已读消息数
     */
    @GetMapping("/selectReadStatus")
    public Integer selectReadStatus(@RequestParam("groupId") String groupId, @RequestParam("toId") String toId, @RequestParam("code") Integer code) {
        return imGroupMessageService.selectReadStatus(groupId, toId, code);
    }

    /**
     * 插入群聊消息状态
     *
     * @param groupReadStatusList 群聊消息群成员阅读状态
     */
    @PostMapping("/status/batch/insert")
    public Boolean groupMessageStatusBatchInsert(@RequestBody List<ImGroupMessageStatusPo> groupReadStatusList) {
        return imGroupMessageStatusService.saveBatch(groupReadStatusList);
    }
}