package com.xy.database.controller;

import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImGroupMessageService;
import com.xy.database.service.ImGroupMessageStatusService;
import com.xy.domain.po.ImGroupMessagePo;
import com.xy.domain.po.ImGroupMessageStatusPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/group/message")
@Tag(name = "ImGroup", description = "群聊消息数据库接口")
@RequiredArgsConstructor
public class ImGroupMessageController {

    private final ImGroupMessageService imGroupMessageService;
    private final ImGroupMessageStatusService imGroupMessageStatusService;

    /**
     * 获取用户私聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/list")
    public List<ImGroupMessagePo> list(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imGroupMessageService.list(userId, sequence);
    }

    /**
     * 插入群聊消息
     *
     * @param messagePo 群聊消息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImGroupMessagePo messagePo) {
        return imGroupMessageService.save(messagePo);
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
