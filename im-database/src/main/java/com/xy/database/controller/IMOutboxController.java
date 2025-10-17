package com.xy.database.controller;


import com.xy.database.service.IMOutboxService;
import com.xy.domain.po.IMOutboxPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/outbox")
@Tag(name = "IMOutbox", description = "用户会话数据库接口")
@RequiredArgsConstructor
public class IMOutboxController {

    private final IMOutboxService imOutboxService;

    /**
     * 查询消息列表
     *
     * @return 消息列表
     */
    @GetMapping("/selectList")
    public List<IMOutboxPo> selectList() {
        return imOutboxService.selectList();
    }

    /**
     * 获取单个
     *
     * @param id
     * @return
     */
    @GetMapping("/selectOne")
    public IMOutboxPo selectOne(@RequestParam("id") Long id) {
        return imOutboxService.selectById(id);
    }

    /**
     * 保存或更新
     *
     * @param outboxPo
     * @return
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody IMOutboxPo outboxPo) {
        return imOutboxService.insert(outboxPo);
    }

    /**
     * 批量插入消息
     *
     * @param outboxPoList 消息列表
     * @return 是否插入成功
     */
    @PostMapping("/batchInsert")
    public Boolean batchInsert(@RequestBody List<IMOutboxPo> outboxPoList) {
        return imOutboxService.batchInsert(outboxPoList);
    }

    /**
     * 更新消息
     *
     * @param outboxPo 消息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody IMOutboxPo outboxPo) {
        return imOutboxService.update(outboxPo);
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("id") Long id) {
        return imOutboxService.deleteById(id);
    }

    /**
     * 批量获取待发送的消息
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 消息列表
     */
    @GetMapping("/selectListByStatus")
    public List<IMOutboxPo> selectListByStatus(@RequestParam("status") String status, @RequestParam("limit") Integer limit) {
        return imOutboxService.listByStatus(status, limit);
    }

    /**
     * 更新消息状态
     *
     * @param id       消息ID
     * @param status   状态
     * @param attempts 尝试次数
     * @return 是否更新成功
     */
    @PutMapping("/updateStatus")
    public Boolean updateStatus(@RequestParam("id") Long id, @RequestParam("status") String status, @RequestParam("attempts") Integer attempts) {
        return imOutboxService.updateStatus(id, status, attempts);
    }

    /**
     * 更新消息为发送失败
     *
     * @param id        消息ID
     * @param lastError 错误信息
     * @param attempts  尝试次数
     * @return 是否更新成功
     */
    @PutMapping("/markAsFailed")
    public Boolean markAsFailed(@RequestParam("id") Long id, @RequestParam("lastError") String lastError, @RequestParam("attempts") Integer attempts) {
        return imOutboxService.markAsFailed(id, lastError, attempts);
    }
}