package com.xy.lucky.dubbo.webflux.api.database.outbox;

import com.xy.lucky.domain.po.IMOutboxPo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 发件箱 Dubbo WebFlux 服务接口
 * <p>
 * 提供发件箱的响应式 CRUD 操作
 *
 * @author lucky
 * @since 1.0.0
 */
public interface ImOutboxDubboService {

    /**
     * 查询所有发件箱记录
     *
     * @return 发件箱记录 Flux
     */
    Flux<IMOutboxPo> queryList();

    /**
     * 根据 ID 查询发件箱记录
     *
     * @param id 记录 ID
     * @return 发件箱记录 Mono
     */
    Mono<IMOutboxPo> queryOne(Long id);

    /**
     * 创建发件箱记录
     *
     * @param po 记录对象
     * @return 是否创建成功 Mono
     */
    Mono<Boolean> create(IMOutboxPo po);

    /**
     * 批量创建发件箱记录
     *
     * @param list 记录对象列表
     * @return 是否全部创建成功 Mono
     */
    Mono<Boolean> createBatch(List<IMOutboxPo> list);

    /**
     * 修改发件箱记录
     *
     * @param po 记录对象
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modify(IMOutboxPo po);

    /**
     * 删除发件箱记录
     *
     * @param id 记录 ID
     * @return 是否删除成功 Mono
     */
    Mono<Boolean> removeOne(Long id);

    /**
     * 根据状态查询发件箱记录
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 发件箱记录 Flux
     */
    Flux<IMOutboxPo> queryByStatus(String status, Integer limit);

    /**
     * 修改发件箱记录状态
     *
     * @param id       记录 ID
     * @param status   新状态
     * @param attempts 重试次数
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modifyStatus(Long id, String status, Integer attempts);

    /**
     * 修改发件箱记录为失败状态
     *
     * @param id        记录 ID
     * @param lastError 最后一次错误信息
     * @param attempts  重试次数
     * @return 是否修改成功 Mono
     */
    Mono<Boolean> modifyToFailed(Long id, String lastError, Integer attempts);
}
