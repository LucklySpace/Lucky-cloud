package com.xy.server.api.database.user;


import com.xy.domain.po.ImUserDataPo;
import com.xy.server.api.FeignRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "user", value = "im-database", path = "/api/v1/database/user", configuration = FeignRequestInterceptor.class)
public interface ImUserFeign {


    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息集合
     */
    @PostMapping("/data/getOne")
    ImUserDataPo getOne(@RequestParam("userId") String userId);

    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/data/getUserByIds")
    List<ImUserDataPo> getUserByIds(@RequestBody List<String> userIdList);

}
