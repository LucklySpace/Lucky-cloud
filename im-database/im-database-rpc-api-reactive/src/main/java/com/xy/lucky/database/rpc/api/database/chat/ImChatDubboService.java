package com.xy.lucky.database.rpc.api.database.chat;

import com.xy.lucky.domain.po.ImChatPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 会话 Dubbo WebFlux 服务接口
 * <p>
 * 提供会话的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImChatDubboService {

    /**
     * 查询会话列表
     *
     * @param ownerId  用户 ID
     * @param sequence 序列号（用于增量同步）
     * @return 会话列表 Flux
     */
    Flux<ImChatPo> queryList(String ownerId, Long sequence);

    /**
     * 查询单个会话
     *
     * @param ownerId  用户 ID
     * @param toId     对方 ID（好友 ID 或群组 ID）
     * @param chatType 会话类型（可选）
     * @return 会话信息 Mono
     */
    Mono<ImChatPo> queryOne(String ownerId, String toId, Integer chatType);

    /**
     * 创建会话
     *
     * @param chatPo 会话对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImChatPo chatPo);

    /**
     * 修改会话
     *
     * @param chatPo 会话对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImChatPo chatPo);

    /**
     * 保存或更新会话
     *
     * @param chatPo 会话对象
     * @return 是否成功 Mono
     */
    Mono<Boolean> saveOrUpdate(ImChatPo chatPo);

    /**
     * 删除会话
     *
     * @param id 会话 ID（表主键）
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String id);
}
