package com.xy.lucky.database.rpc.api.database.user;

import com.xy.lucky.domain.po.ImUserDataPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 用户资料 Dubbo WebFlux 服务接口
 * <p>
 * 提供用户扩展资料的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImUserDataDubboService {

    /**
     * 根据关键词模糊查询用户资料
     *
     * @param keyword 关键词（用户ID或名称等）
     * @return 用户资料 Flux
     */
    Flux<ImUserDataPo> queryByKeyword(String keyword);

    /**
     * 根据用户 ID 查询用户资料
     *
     * @param userId 用户 ID
     * @return 用户资料 Mono
     */
    Mono<ImUserDataPo> queryOne(String userId);

    /**
     * 修改用户资料
     *
     * @param po 用户资料对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(ImUserDataPo po);

    /**
     * 根据用户 ID 列表批量查询用户资料
     *
     * @param userIdList 用户 ID 列表
     * @return 用户资料 Flux
     */
    Flux<ImUserDataPo> queryListByIds(List<String> userIdList);
}
