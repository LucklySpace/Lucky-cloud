package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImGroupMessagePo;

import java.util.List;


/**
 * @author dense
 * @description 针对表【im_group_message】的数据库操作Service
 */
public interface ImGroupMessageService extends IService<ImGroupMessagePo> {

    /**
     * 插入群消息
     * @param groupMessagePo 群消息
     * @return 是否成功
     */
    boolean insert(ImGroupMessagePo groupMessagePo);

    /**
     * 批量插入群消息
     * @param groupMessagePoList 群消息列表
     * @return 是否成功
     */
    boolean batchInsert(List<ImGroupMessagePo> groupMessagePoList);

    /**
     * 查询单条群消息
     * @param messageId 消息ID
     * @return 群消息
     */
    ImGroupMessagePo selectOne(String messageId);

    /**
     * 更新群消息
     * @param groupMessagePo 群消息
     * @return 是否成功
     */
    boolean update(ImGroupMessagePo groupMessagePo);
    
    /**
     * 根据ID删除群消息
     * @param messageId 消息ID
     * @return 是否成功
     */
    boolean deleteById(String messageId);

    List<ImGroupMessagePo> selectList(String userId, Long sequence);

    ImGroupMessagePo last(String userId, String groupId);

    Integer selectReadStatus(String groupId, String toId, Integer code);
}