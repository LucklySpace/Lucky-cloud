package com.xy.lucky.dubbo.webflux.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipGroupMemberPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 好友分组成员 Dubbo WebFlux 服务接口
 * <p>
 * 提供好友分组成员的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImFriendshipGroupMemberDubboService {

    /**
     * 查询分组内的成员列表
     *
     * @param groupId 分组 ID
     * @return 成员列表 Flux
     */
    Flux<ImFriendshipGroupMemberPo> queryList(String groupId);

    /**
     * 查询单个分组成员
     *
     * @param groupId  分组 ID
     * @param memberId 成员 ID
     * @return 分组成员信息 Mono
     */
    Mono<ImFriendshipGroupMemberPo> queryOne(String groupId, String memberId);

    /**
     * 添加分组成员
     *
     * @param memberPo 分组成员对象
     * @return 是否添加成功 Mono
     */
    Mono<Boolean> create(ImFriendshipGroupMemberPo memberPo);

    /**
     * 批量添加分组成员
     *
     * @param memberPoList 分组成员列表
     * @return 是否全部添加成功 Mono
     */
    Mono<Boolean> createBatch(List<ImFriendshipGroupMemberPo> memberPoList);

    /**
     * 修改分组成员信息
     *
     * @param memberPo 分组成员对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImFriendshipGroupMemberPo memberPo);

    /**
     * 移除分组成员
     *
     * @param memberId 成员 ID
     * @return 是否移除成功 Mono
     */
    Mono<Boolean> removeOne(String memberId);
}
