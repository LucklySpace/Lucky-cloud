package com.xy.lucky.database.rpc.api.database.message;

import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 群聊消息 Dubbo WebFlux 服务接口
 * <p>
 * 提供群聊消息的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImGroupMessageDubboService {

    /**
     * 查询群聊消息列表
     *
     * @param userId   用户 ID
     * @param sequence 序列号（用于增量同步）
     * @return 消息列表 Flux
     */
    Flux<ImGroupMessagePo> queryList(String userId, Long sequence);

    /**
     * 根据消息 ID 查询消息
     *
     * @param messageId 消息 ID
     * @return 消息信息 Mono
     */
    Mono<ImGroupMessagePo> queryOne(String messageId);

    /**
     * 创建群聊消息
     *
     * @param groupMessagePo 消息对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImGroupMessagePo groupMessagePo);

    /**
     * 批量创建群聊消息状态
     *
     * @param groupMessageStatusPoList 消息状态列表
     * @return 是否全部创建成功 Mono
     */
    Mono<Boolean> createBatch(List<ImGroupMessageStatusPo> groupMessageStatusPoList);

    /**
     * 修改群聊消息
     *
     * @param groupMessagePo 消息对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImGroupMessagePo groupMessagePo);

    /**
     * 删除群聊消息
     *
     * @param messageId 消息 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String messageId);

    /**
     * 查询群组内的最后一条消息
     *
     * @param groupId 群组 ID
     * @param userId  用户 ID
     * @return 最后一条消息 Mono
     */
    Mono<ImGroupMessagePo> queryLast(String groupId, String userId);

    /**
     * 查询群聊消息已读状态
     *
     * @param groupId 群组 ID
     * @param toId    接收者 ID
     * @param code    状态码
     * @return 状态数量 Mono
     */
    Mono<Integer> queryReadStatus(String groupId, String toId, Integer code);
}
