package com.xy.lucky.dubbo.webflux.api.database.group;

import com.xy.lucky.domain.po.ImGroupPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 群组 Dubbo WebFlux 服务接口
 * <p>
 * 提供群组的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImGroupDubboService {

    /**
     * 查询用户加入的群组列表
     *
     * @param userId 用户 ID
     * @return 群组列表 Flux
     */
    Flux<ImGroupPo> queryList(String userId);

    /**
     * 根据群组 ID 查询群组信息
     *
     * @param groupId 群组 ID
     * @return 群组信息 Mono
     */
    Mono<ImGroupPo> queryOne(String groupId);

    /**
     * 创建群组
     *
     * @param groupPo 群组对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImGroupPo groupPo);

    /**
     * 批量创建群组
     *
     * @param list 群组对象列表
     * @return 是否全部创建成功 Mono
     */
    Mono<Boolean> createBatch(List<ImGroupPo> list);

    /**
     * 修改群组信息
     *
     * @param groupPo 群组对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImGroupPo groupPo);

    /**
     * 删除群组
     *
     * @param groupId 群组 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String groupId);
}
