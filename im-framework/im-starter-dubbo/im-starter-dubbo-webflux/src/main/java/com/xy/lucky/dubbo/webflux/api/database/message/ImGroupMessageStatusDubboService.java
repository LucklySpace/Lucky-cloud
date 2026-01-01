package com.xy.lucky.dubbo.webflux.api.database.message;

import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 群消息状态 Dubbo WebFlux 服务接口
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImGroupMessageStatusDubboService {

    /**
     * 保存或更新群消息状态
     *
     * @param statusPo 状态对象
     * @return 是否成功 Mono
     */
    Mono<Boolean> saveOrUpdate(ImGroupMessageStatusPo statusPo);

    /**
     * 批量保存或更新
     *
     * @param list 状态对象列表
     * @return 是否成功 Mono
     */
    Mono<Boolean> saveOrUpdateBatch(List<ImGroupMessageStatusPo> list);

    /**
     * 查询单个消息状态
     *
     * @param groupId   群组ID
     * @param messageId 消息ID
     * @param toId      接收者ID
     * @return 状态对象 Mono
     */
    Mono<ImGroupMessageStatusPo> queryOne(String groupId, String messageId, String toId);

    /**
     * 统计群消息已读人数
     *
     * @param groupId   群组ID
     * @param messageId 消息ID
     * @return 已读人数 Mono
     */
    Mono<Long> countRead(String groupId, String messageId);
}
