package com.xy.server.api.feign.database.friend;


import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.server.api.feign.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 用户关系
 */
@FeignClient(contextId = "friend", value = "im-database", path = "/api/v1/database", configuration = FeignRequestInterceptor.class)
public interface ImRelationshipFeign {

    /**
     * 根据时间序列查询好友
     *
     * @param ownerId 用户id
     * @return 用户好友信息列表
     */
    @GetMapping("/friend/selectList")
    List<ImFriendshipPo> contacts(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence);


    /**
     * 根据时间序列查询好友
     *
     * @param userId 用户id
     * @return 用户好友信息列表
     */
    @GetMapping("/group/selectList")
    List<ImGroupPo> group(@RequestParam("userId") String userId);


    /**
     * 根据用户和好友id查询
     *
     * @param ownerId
     * @param toId
     * @return
     */
    @GetMapping("/friend/ship/selectOne")
    ImFriendshipPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId);


    @GetMapping("/friend/ship/selectList")
    List<ImFriendshipPo> shipList(@RequestParam("ownerId") String ownerId, @RequestParam("ids") List<String> ids);

    /**
     * 请求好友列表
     *
     * @return
     */
    @GetMapping("/friend/request/selectList")
    List<ImFriendshipRequestPo> newFriends(@RequestParam("userId") String userId);

    /**
     * 获取好友请求
     *
     * @param request
     * @return
     */
    @PostMapping("/friend/request/selectOne")
    ImFriendshipRequestPo getRequestOne(@RequestBody ImFriendshipRequestPo request);

    /**
     * 添加好友请求
     *
     * @param request 好友请求信息
     */
    @PostMapping("/friend/request/add")
    void addFriendRequest(@RequestBody ImFriendshipRequestPo request);

    /**
     * 更新好友请求状态
     *
     * @param request 好友请求信息
     */
    @PostMapping("/friend/request/update")
    void updateFriendRequest(@RequestBody ImFriendshipRequestPo request);

    /**
     * 更新好友请求状态
     *
     * @param requestId 请求ID
     * @param status    审批状态
     */
    @PutMapping("/friend/request/updateStatus")
    void updateFriendRequestStatus(@RequestParam("requestId") String requestId, @RequestParam("status") Integer status);

    /**
     * 创建好友关系
     *
     * @param friendship 好友关系信息
     */
    @PostMapping("/friend/create")
    void createFriendship(@RequestBody ImFriendshipPo friendship);

    /**
     * 删除好友关系
     *
     * @param ownerId  用户ID
     * @param friendId 好友ID
     */
    @PostMapping("/friend/delete")
    Boolean deleteFriendship(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId);
}