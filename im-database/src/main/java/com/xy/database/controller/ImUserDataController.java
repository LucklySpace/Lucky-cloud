package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImUserDataService;
import com.xy.domain.po.ImUserDataPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/user/data")
@Tag(name = "ImUserData", description = "用户数据数据库接口")
@RequiredArgsConstructor
public class ImUserDataController {

    private final ImUserDataService imUserDataService;

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息集合
     */
    @GetMapping("/getOne")
    public ImUserDataPo getOne(@RequestParam("userId") String userId) {
        return imUserDataService.getById(userId);
    }


    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/getUserByIds")
    public List<ImUserDataPo> getUserByIds(@RequestBody List<String> userIdList) {
        return imUserDataService.listByIds(userIdList);
    }
}
