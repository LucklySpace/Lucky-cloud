package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.IMOutboxPo;

import java.util.List;

public interface IMOutboxService extends IService<IMOutboxPo> {
    
    /**
     * 插入消息
     * @param outboxPo 消息
     * @return 是否成功
     */
    boolean insert(IMOutboxPo outboxPo);

    /**
     * 批量插入消息
     * @param outboxPoList 消息列表
     * @return 是否成功
     */
    boolean batchInsert(List<IMOutboxPo> outboxPoList);

    /**
     * 查询单条消息
     * @param id 消息ID
     * @return 消息
     */
    IMOutboxPo selectOne(Long id);
    
    /**
     * 根据ID查询消息
     * @param id 消息ID
     * @return 消息
     */
    IMOutboxPo selectById(Long id);

    /**
     * 查询消息列表
     * @return 消息列表
     */
    List<IMOutboxPo> selectList();
    
    /**
     * 更新消息
     * @param outboxPo 消息
     * @return 是否成功
     */
    boolean update(IMOutboxPo outboxPo);
    
    /**
     * 根据ID删除消息
     * @param id 消息ID
     * @return 是否成功
     */
    boolean deleteById(Long id);
    
    Boolean updateStatus(Long id, String status, Integer attempts);

    Boolean markAsFailed(Long id, String lastError, Integer attempts);

    List<IMOutboxPo> listByStatus(String status, Integer limit);
}