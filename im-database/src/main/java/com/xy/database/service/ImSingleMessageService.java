package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImSingleMessagePo;

import java.util.List;


/**
 * @author dense
 * @description 针对表【im_single_message】的数据库操作Service
 * @createDate 2024-03-28 23:00:15
 */
public interface ImSingleMessageService extends IService<ImSingleMessagePo> {

    /**
     * 查询单聊消息列表
     * @return 单聊消息列表
     */
    List<ImSingleMessagePo> selectList(String userId, Long sequence);

    /**
     * 查询单条单聊消息
     * @param messageId 消息ID
     * @return 单聊消息
     */
    ImSingleMessagePo selectOne(String messageId);

    /**
     * 插入单聊消息
     * @param singleMessagePo 单聊消息
     * @return 是否成功
     */
    boolean insert(ImSingleMessagePo singleMessagePo);

    /**
     * 批量插入单聊消息
     * @param singleMessagePoList 单聊消息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImSingleMessagePo> singleMessagePoList);

    /**
     * 更新单聊消息
     * @param singleMessagePo 单聊消息
     * @return 是否成功
     */
    boolean update(ImSingleMessagePo singleMessagePo);
    
    /**
     * 根据ID删除单聊消息
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean deleteById(String messageId);



    ImSingleMessagePo last(String fromId, String toId);

    Integer selectReadStatus(String fromId, String toId, Integer code);
}