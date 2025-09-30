package com.xy.server.api.feign.database.outbox;

import com.xy.domain.po.IMOutboxPo;
import com.xy.server.api.feign.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * mq 消息表 用于保证消息是否发送成功
 */
@FeignClient(contextId = "outbox", value = "im-database", path = "/api/v1/database/outbox", configuration = FeignRequestInterceptor.class)
public interface IMOutboxFeign {

    /**
     * 获取单个
     *
     * @param id
     */
    @GetMapping("/getOne")
    IMOutboxPo getOne(@RequestParam("id") Long id);

    /**
     * 保存或更新
     *
     * @param outboxPo
     */
    @PostMapping("/saveOrUpdate")
    Boolean saveOrUpdate(@RequestBody IMOutboxPo outboxPo);

    /**
     * 删除
     *
     * @param outboxPo
     */
    @PostMapping("/delete")
    Boolean delete(@RequestBody IMOutboxPo outboxPo);

    /**
     * 批量获取待发送的消息
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 消息列表
     */
    @GetMapping("/listByStatus")
    List<IMOutboxPo> listByStatus(@RequestParam("status") String status, @RequestParam("limit") Integer limit);

    /**
     * 更新消息状态
     *
     * @param id       消息ID
     * @param status   状态
     * @param attempts 尝试次数
     * @return 是否更新成功
     */
    @PostMapping("/updateStatus")
    Boolean updateStatus(@RequestParam("id") Long id, @RequestParam("status") String status, @RequestParam("attempts") Integer attempts);

    /**
     * 更新消息为发送失败
     *
     * @param id        消息ID
     * @param lastError 错误信息
     * @param attempts  尝试次数
     * @return 是否更新成功
     */
    @PostMapping("/markAsFailed")
    Boolean markAsFailed(@RequestParam("id") Long id, @RequestParam("lastError") String lastError, @RequestParam("attempts") Integer attempts);
}