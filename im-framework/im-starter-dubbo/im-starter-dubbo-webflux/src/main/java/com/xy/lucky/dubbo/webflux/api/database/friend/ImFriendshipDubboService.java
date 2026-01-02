package com.xy.lucky.dubbo.webflux.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 好友关系 Dubbo WebFlux 服务接口
 * <p>
 * 提供好友关系的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImFriendshipDubboService {

    /**
     * 查询好友列表
     *
     * @param ownerId  用户 ID
     * @param sequence 序列号（用于增量同步）
     * @return 好友关系 Flux
     */
    Flux<ImFriendshipPo> queryList(String ownerId, Long sequence);

    /**
     * 根据好友 ID 列表查询好友关系
     *
     * @param ownerId 用户 ID
     * @param ids     好友 ID 列表
     * @return 好友关系 Flux
     */
    Flux<ImFriendshipPo> queryListByIds(String ownerId, List<String> ids);

    /**
     * 查询单个好友关系
     *
     * @param ownerId 用户 ID
     * @param toId    好友 ID
     * @return 好友关系 Mono
     */
    Mono<ImFriendshipPo> queryOne(String ownerId, String toId);

    /**
     * 创建好友关系
     *
     * @param friendship 好友关系对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImFriendshipPo friendship);

    /**
     * 修改好友关系
     *
     * @param friendship 好友关系对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImFriendshipPo friendship);

    /**
     * 删除好友关系
     *
     * @param ownerId  用户 ID
     * @param friendId 好友 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String ownerId, String friendId);


    /**
     * 创建好友关系列表
     *
     * @param friendshipPoList 好友关系列表
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> createBatch(List<ImFriendshipPo> friendshipPoList);
}
