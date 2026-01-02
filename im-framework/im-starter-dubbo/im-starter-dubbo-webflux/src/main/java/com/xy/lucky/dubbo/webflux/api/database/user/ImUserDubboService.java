package com.xy.lucky.dubbo.webflux.api.database.user;

import com.xy.lucky.domain.po.ImUserPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 用户 Dubbo WebFlux 服务接口
 * <p>
 * 提供用户基础信息的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImUserDubboService {

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息 Mono
     */
    Mono<ImUserPo> queryOne(String userId);

    /**
     * 根据手机号查询用户信息
     *
     * @param mobile 手机号
     * @return 用户信息 Mono
     */
    Mono<ImUserPo> queryOneByMobile(String mobile);

    /**
     * 创建用户
     *
     * @param userPo 用户信息对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(ImUserPo userPo);

    /**
     * 批量创建用户
     *
     * @param userPoList 用户信息列表
     * @return 是否全部创建成功 Mono
     */
    Mono<Boolean> createBatch(List<ImUserPo> userPoList);

    /**
     * 修改用户信息
     *
     * @param userPo 用户信息对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImUserPo userPo);

    /**
     * 根据用户 ID 删除用户
     *
     * @param userId 用户 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(String userId);

    /**
     * 根据用户 ID 列表批量查询用户信息
     *
     * @param userIds 用户 ID 列表
     * @return 用户信息 Flux
     */
    Flux<ImUserPo> listByIds(List<String> userIds);

    /**
     * 统计用户总数
     *
     * @return 用户总数 Mono
     */
    Mono<Long> count();
}
