package com.xy.lucky.dubbo.api.database.friend;

import com.xy.lucky.domain.po.ImFriendshipRequestPo;

import java.util.List;

/**
 * 好友关系Dubbo服务接口
 */
public interface ImFriendshipRequestDubboService {

    /**
     * 请求好友列表
     *
     * @param userId 用户ID
     * @return 好友请求列表
     */
    List<ImFriendshipRequestPo> queryList(String userId);

    /**
     * 获取好友请求
     *
     * @param request 好友请求信息
     * @return 好友请求信息
     */
    ImFriendshipRequestPo queryOne(ImFriendshipRequestPo request);

    /**
     * 添加好友请求
     *
     * @param request 好友请求信息
     */
    Boolean creat(ImFriendshipRequestPo request);

    /**
     * 更新好友请求
     *
     * @param request 好友请求信息
     */
    Boolean modify(ImFriendshipRequestPo request);

    /**
     * 删除好友请求
     *
     * @param requestId 请求ID
     */
    Boolean removeOne(String requestId);

    /**
     * 更新好友请求状态
     *
     * @param requestId 请求ID
     * @param status    审批状态
     */
    Boolean modifyStatus(String requestId, Integer status);

}
