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
     * 获取单个
     *
     * @param id
     * @return
     */
    @GetMapping("/getOne")
    public IMOutboxPo getOne(@RequestParam("id") Long id) {
        return imOutboxService.getById(id);
    }

    /**
     * 保存或更新
     *
     * @param outboxPo
     * @return
     */
    @PostMapping("/saveOrUpdate")
    public Boolean saveOrUpdate(@RequestBody IMOutboxPo outboxPo) {
        return imOutboxService.saveOrUpdate(outboxPo);
    }

    /**
     * 删除
     *
     * @param outboxPo
     * @return
     */
    @PostMapping("/delete")
    public Boolean delete(@RequestBody IMOutboxPo outboxPo) {
        return imOutboxService.removeById(outboxPo);
    }

    /**
     * 批量获取待发送的消息
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 消息列表
     */
    @GetMapping("/listByStatus")
    public List<IMOutboxPo> listByStatus(@RequestParam("status") String status, @RequestParam("limit") Integer limit) {
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
    @PostMapping("/updateStatus")
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
    @PostMapping("/markAsFailed")
    public Boolean markAsFailed(@RequestParam("id") Long id, @RequestParam("lastError") String lastError, @RequestParam("attempts") Integer attempts) {
        return imOutboxService.markAsFailed(id, lastError, attempts);
    }
}
