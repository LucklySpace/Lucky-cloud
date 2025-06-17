package com.xy.server.api.database.friend;


import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.server.api.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "friend", value = "im-database", path = "/api/v1/database/friend", configuration = FeignRequestInterceptor.class)
public interface ImFriendFeign {



    @GetMapping("list")
    List<ImFriendshipPo> selectList(@RequestParam("owner_id") String userId, @RequestParam("sequence") Long sequence);

}
