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
    @GetMapping("/selectOne")
    public ImUserDataPo selectOne(@RequestParam("userId") String userId) {
        return imUserDataService.selectById(userId);
    }


    /**
     * 批量获取用户
     *
     * @param userIdList 用户id集合
     * @return 用户信息集合
     */
    @PostMapping("/selectListByIds")
    public List<ImUserDataPo> selectListByIds(@RequestBody List<String> userIdList) {
        return imUserDataService.listByIds(userIdList);
    }

    /**
     * 更新用户信息
     *
     * @param po 用户信息
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public boolean update(@RequestBody ImUserDataPo po) {
        return imUserDataService.updateById(po);
    }
    
    /**
     * 插入用户数据信息
     *
     * @param userDataPo 用户数据信息
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImUserDataPo userDataPo) {
        return imUserDataService.insert(userDataPo);
    }
    
    /**
     * 批量插入用户数据信息
     *
     * @param userDataPoList 用户数据信息列表
     * @return 是否插入成功
     */
    @PostMapping("/batchInsert")
    public Boolean batchInsert(@RequestBody List<ImUserDataPo> userDataPoList) {
        return imUserDataService.batchInsert(userDataPoList);
    }

    /**
     * 删除用户数据信息
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("userId") String userId) {
        return imUserDataService.deleteById(userId);
    }
}