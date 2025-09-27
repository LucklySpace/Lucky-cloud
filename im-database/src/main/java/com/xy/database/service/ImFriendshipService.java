package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_friendship】的数据库操作Service
 */
public interface ImFriendshipService extends IService<ImFriendshipPo> {

    List<ImFriendshipPo> list(String ownerId);


    ImFriendshipPo getOne(String ownerId, String toId);

    /**
     * 保存好友请求
     *
     * @param request 好友请求信息
     */
    void saveFriendRequest(ImFriendshipRequestPo request);

    /**
     * 更新好友请求状态
     *
     * @param requestId 请求ID
     * @param status    审批状态
     */
    void updateFriendRequestStatus(String requestId, Integer status);

    /**
     * 保存好友关系
     *
     * @param friendship 好友关系信息
     */
    void saveFriendship(ImFriendshipPo friendship);

    /**
     * 删除好友关系
     *
     * @param ownerId  用户ID
     * @param friendId 好友ID
     */
    void deleteFriendship(String ownerId, String friendId);

    /**
     * 批量查询好友关系
     *
     * @param ownerId 用户ID
     * @param ids     好友ID列表
     * @return 好友关系列表
     */
    List<ImFriendshipPo> getFriendshipList(String ownerId, List<String> ids);

    void updateFriendRequest(ImFriendshipRequestPo request);
}