package com.xy.database.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImUserService;
import com.xy.domain.po.ImUserPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/user")
@Tag(name = "ImUser", description = "用户数据库接口")
@RequiredArgsConstructor
public class ImUserController {

    private final ImUserService imUserService;

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息集合
     */
    @GetMapping("/getOne")
    public ImUserPo getOne(@RequestParam("userId") String userId) {
        ImUserPo byId = imUserService.getById(userId);
        return byId;
    }


    /**
     * 获取用户信息
     *
     * @param mobile 用户手机号
     * @return 用户信息集合
     */
    @GetMapping("/getOneByMobile")
    public ImUserPo getOneByMobile(@RequestParam("mobile") String mobile) {
        // 使用select方法只查询需要的字段，避免加载整个实体对象
        QueryWrapper<ImUserPo> wrapper = new QueryWrapper<>();
        wrapper.eq("mobile", mobile);
        return imUserService.getOne(wrapper);
    }


    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/getUserByIds")
    public List<ImUserPo> getUserByIds(@RequestBody List<String> userIdList) {
        return imUserService.listByIds(userIdList);
    }
}
