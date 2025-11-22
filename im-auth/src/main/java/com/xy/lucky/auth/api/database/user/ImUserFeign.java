package com.xy.lucky.auth.api.database.user;


import com.xy.lucky.auth.api.FeignRequestInterceptor;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.po.ImUserPo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
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
    @GetMapping("/selectOne")
    ImUserPo getOneUser(@RequestParam("userId") String userId);

    /**
     * 获取用户信息
     *
     * @param mobile 用户手机号
     * @return 用户信息集合
     */
    @GetMapping("/getOneByMobile")
    ImUserPo getOneByMobile(@RequestParam("mobile") String mobile);

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息集合
     */
    @GetMapping("/data/selectOne")
    ImUserDataPo getOneUserData(@RequestParam("userId") String userId);

    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/data/getUserByIds")
    List<ImUserDataPo> getUserByIds(@RequestBody List<String> userIdList);

}
