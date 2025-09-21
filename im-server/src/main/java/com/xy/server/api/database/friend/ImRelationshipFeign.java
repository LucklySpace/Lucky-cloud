package com.xy.server.api.database.friend;


import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.server.api.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    @GetMapping("/friend/list")
    List<ImFriendshipPo> contacts(@RequestParam("ownerId") String ownerId);


    /**
     * 根据时间序列查询好友
     *
     * @param userId 用户id
     * @return 用户好友信息列表
     */
    @GetMapping("/group/list")
    List<ImGroupPo> group(@RequestParam("userId") String userId);


    /**
     * 根据用户和好友id查询
     *
     * @param ownerId
     * @param friendId
     * @return
     */
    @GetMapping("/friend/getOne")
    ImFriendshipPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId);

    @GetMapping("/friend/request/list")
    List<ImFriendshipRequestPo> newFriends(@RequestParam("userId") String userId);
}
