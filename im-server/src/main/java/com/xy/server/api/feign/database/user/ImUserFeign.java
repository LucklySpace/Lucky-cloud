package com.xy.server.api.feign.database.user;


import com.xy.domain.po.ImUserDataPo;
import com.xy.server.api.feign.FeignRequestInterceptor;
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
    @GetMapping("/data/selectOne")
    ImUserDataPo getOne(@RequestParam("userId") String userId);


    /**
     * 创建用户信息
     *
     * @param userDataPo 用户信息
     * @return 用户信息
     */
    @PostMapping("/data/insert")
    Boolean insert(@RequestBody ImUserDataPo userDataPo);

    /**
     * 更新用户信息
     *
     * @param userDataPo 用户信息
     * @return 用户信息
     */
    @PostMapping("/data/update")
    Boolean update(@RequestBody ImUserDataPo userDataPo);


    /**
     * 获取用户信息
     *
     * @param keyword 关键词
     * @return 用户信息集合
     */
    @GetMapping("/search")
    List<ImUserDataPo> search(@RequestParam("keyword") String keyword);

    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/data/selectListByIds")
    List<ImUserDataPo> selectListByIds(@RequestBody List<String> userIdList);

}