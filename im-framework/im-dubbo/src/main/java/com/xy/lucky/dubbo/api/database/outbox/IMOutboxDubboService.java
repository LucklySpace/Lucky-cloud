package com.xy.lucky.dubbo.api.database.outbox;

import com.xy.lucky.domain.po.IMOutboxPo;

import java.util.List;

/**
 * mq 消息表 Dubbo服务接口，用于保证消息是否发送成功
 */
public interface IMOutboxDubboService {

    /**
     * 获取消息列表
     *
     * @return 消息列表
     */
    List<IMOutboxPo> selectList();


    /**
     * 获取单个消息
     *
     * @param id 消息ID
     * @return 消息信息
     */
    IMOutboxPo selectOne(Long id);

    /**
     * 保存消息
     *
     * @param outboxPo 消息信息
     * @return 是否成功
     */
    Boolean insert(IMOutboxPo outboxPo);

    /**
     * 批量保存消息
     *
     * @param list 待保存的消息列表
     * @return 是否成功
     */
    Boolean batchInsert(List<IMOutboxPo> list);

    /**
     * 更新消息
     *
     * @param outboxPo 待更新的消息信息
     * @return 是否成功
     */
    Boolean update(IMOutboxPo outboxPo);

    /**
     * 保存或更新消息
     *
     * @param outboxPo 消息信息
     * @return 是否成功
     */
    boolean saveOrUpdate(IMOutboxPo outboxPo);

    /**
     * 删除消息
     *
     * @param id id
     * @return 是否成功
     */
    Boolean deleteById(Long id);

    /**
     * 批量获取待发送的消息
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 消息列表
     */
    List<IMOutboxPo> listByStatus(String status, Integer limit);

    /**
     * 更新消息状态
     *
     * @param id       消息ID
     * @param status   状态
     * @param attempts 尝试次数
     * @return 是否更新成功
     */
    Boolean updateStatus(Long id, String status, Integer attempts);

    /**
     * 更新消息为发送失败
     *
     * @param id        消息ID
     * @param lastError 错误信息
     * @param attempts  尝试次数
     * @return 是否更新成功
     */
    Boolean markAsFailed(Long id, String lastError, Integer attempts);
}