package com.xy.lucky.database.rpc.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipGroupPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 好友分组 Dubbo WebFlux 服务接口
 * <p>
 * 提供好友分组的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImFriendshipGroupDubboService {

    /**
     * 查询用户的所有分组
     *
     * @param ownerId 用户 ID
     * @return 分组列表 Flux
     */
    Flux<ImFriendshipGroupPo> queryList(String ownerId);

    /**
     * 根据分组 ID 查询分组信息
     *
     * @param id 分组 ID
     * @return 分组信息 Mono
     */
    Mono<ImFriendshipGroupPo> queryOne(String id);

    /**
     * 创建分组
     *
     * @param friendshipGroupPo 分组对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImFriendshipGroupPo friendshipGroupPo);

    /**
     * 批量创建分组
     *
     * @param friendshipGroupPoList 分组对象列表
     * @return 是否全部创建成功 Mono
     */
    Mono<Boolean> createBatch(List<ImFriendshipGroupPo> friendshipGroupPoList);

    /**
     * 修改分组信息
     *
     * @param friendshipGroupPo 分组对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImFriendshipGroupPo friendshipGroupPo);

    /**
     * 删除分组
     *
     * @param id 分组 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String id);
}
