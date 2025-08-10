package com.xy.server.api.database.friend;


import com.xy.domain.po.ImFriendshipPo;
import com.xy.server.api.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "friend", value = "im-database", path = "/api/v1/database/friend", configuration = FeignRequestInterceptor.class)
public interface ImFriendFeign {


    /**
     * 根据用户和好友id查询
     * @param ownerId
     * @param friendId
     * @return
     */
    @GetMapping("getOne")
    ImFriendshipPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId);

    /**
     * 根据时间序列查询好友
     * @param ownerId 用户id
     * @param sequence 时序
     * @return 用户好友信息列表
     */
    @GetMapping("list")
    List<ImFriendshipPo> selectList(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence);

}
