package com.xy.lucky.dubbo.webflux.api.database.group;

import com.xy.lucky.domain.po.ImGroupMemberPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 群组成员 Dubbo WebFlux 服务接口
 * <p>
 * 提供群组成员的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImGroupMemberDubboService {

    /**
     * 查询群组内的成员列表
     *
     * @param groupId 群组 ID
     * @return 成员列表 Flux
     */
    Flux<ImGroupMemberPo> queryList(String groupId);

    /**
     * 查询单个群组成员
     *
     * @param groupId  群组 ID
     * @param memberId 成员 ID
     * @return 群组成员信息 Mono
     */
    Mono<ImGroupMemberPo> queryOne(String groupId, String memberId);

    /**
     * 按角色查询群成员
     *
     * @param groupId 群组 ID
     * @param role    角色
     * @return 成员列表 Flux
     */
    Flux<ImGroupMemberPo> queryByRole(String groupId, Integer role);

    /**
     * 查询群组内前 9 名成员的头像（用于生成群头像）
     *
     * @param groupId 群组 ID
     * @return 头像 URL 列表 Mono
     */
    Mono<List<String>> queryNinePeopleAvatar(String groupId);

    /**
     * 添加群组成员
     *
     * @param groupMember 群组成员对象
     * @return 是否添加成功 Mono
     */
    Mono<Boolean> create(ImGroupMemberPo groupMember);

    /**
     * 批量添加群组成员
     *
     * @param groupMemberList 群组成员列表
     * @return 是否全部添加成功 Mono
     */
    Mono<Boolean> createBatch(List<ImGroupMemberPo> groupMemberList);

    /**
     * 修改群组成员信息
     *
     * @param groupMember 群组成员对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImGroupMemberPo groupMember);

    /**
     * 批量修改群组成员信息
     *
     * @param groupMemberList 群组成员列表
     * @return 是否全部修改成功 Mono
     */
    Mono<Boolean> modifyBatch(List<ImGroupMemberPo> groupMemberList);

    /**
     * 移除群组成员
     *
     * @param memberId 成员 ID
     * @return 是否移除成功 Mono
     */
    Mono<Boolean> removeOne(String memberId);

    /**
     * 删除群所有成员
     *
     * @param groupId 群组 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeByGroupId(String groupId);

    /**
     * 统计群成员数量
     *
     * @param groupId 群组 ID
     * @return 成员数量 Mono
     */
    Mono<Long> countByGroupId(String groupId);
}
