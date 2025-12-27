package com.xy.lucky.dubbo.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipPo;

import java.util.List;

/**
 * 好友关系Dubbo服务接口
 */
public interface ImFriendshipDubboService {

    /**
     * 根据时间序列查询好友
     *
     * @param ownerId 用户id
     * @return 用户好友信息列表
     */
    List<ImFriendshipPo> selectList(String ownerId, Long sequence);

    /**
     * 根据用户和好友id查询
     *
     * @param ownerId 用户ID
     * @param toId    好友ID
     * @return 好友关系信息
     */
    ImFriendshipPo selectOne(String ownerId, String toId);

    /**
     * 批量查询好友关系
     *
     * @param ownerId 用户ID
     * @param ids     好友ID列表
     * @return 好友关系列表
     */
    List<ImFriendshipPo> selectByIds(String ownerId, List<String> ids);


    /**
     * 保存好友关系
     *
     * @param friendship 好友关系信息
     * @return 是否保存成功
     */
    Boolean insert(ImFriendshipPo friendship);

    /**
     * 更新好友关系
     *
     * @param friendship 好友关系信息
     * @return 是否保存成功
     */
    Boolean update(ImFriendshipPo friendship);

    /**
     * 删除好友关系
     *
     * @param ownerId  用户ID
     * @param friendId 好友ID
     * @return 是否删除成功
     */
    Boolean delete(String ownerId, String friendId);
}