package com.xy.lucky.dubbo.webflux.api.database.message;

import com.xy.lucky.domain.po.ImSingleMessagePo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 单聊消息 Dubbo WebFlux 服务接口
 * <p>
 * 提供单聊消息的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImSingleMessageDubboService {

    /**
     * 根据消息 ID 查询消息
     *
     * @param messageId 消息 ID
     * @return 消息信息 Mono
     */
    Mono<ImSingleMessagePo> queryOne(String messageId);

    /**
     * 查询单聊消息列表
     *
     * @param userId   用户 ID
     * @param sequence 序列号（用于增量同步）
     * @return 消息列表 Flux
     */
    Flux<ImSingleMessagePo> queryList(String userId, Long sequence);

    /**
     * 查询两人之间的最后一条消息
     *
     * @param fromId 发送者 ID
     * @param toId   接收者 ID
     * @return 最后一条消息 Mono
     */
    Mono<ImSingleMessagePo> queryLast(String fromId, String toId);

    /**
     * 查询单聊消息已读状态
     *
     * @param fromId 发送者 ID
     * @param toId   接收者 ID
     * @param code   状态码
     * @return 状态数量 Mono
     */
    Mono<Integer> queryReadStatus(String fromId, String toId, Integer code);

    /**
     * 创建单聊消息
     *
     * @param singleMessagePo 消息对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImSingleMessagePo singleMessagePo);

    /**
     * 批量创建单聊消息
     *
     * @param singleMessagePoList 消息对象列表
     * @return 是否全部创建成功 Mono
     */
    Mono<Boolean> createBatch(List<ImSingleMessagePo> singleMessagePoList);

    /**
     * 修改单聊消息
     *
     * @param singleMessagePo 消息对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImSingleMessagePo singleMessagePo);

    /**
     * 删除单聊消息
     *
     * @param messageId 消息 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String messageId);

    /**
     * 保存或更新单聊消息
     *
     * @param messagePo 消息对象
     * @return 是否成功 Mono
     */
    Mono<Boolean> saveOrUpdate(ImSingleMessagePo messagePo);
}
