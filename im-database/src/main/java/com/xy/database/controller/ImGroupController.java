package com.xy.database.controller;

import com.xy.database.service.ImGroupService;
import com.xy.domain.po.ImGroupPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/group")
@Tag(name = "ImGroup", description = "群数据库接口")
public class ImGroupController {

    @Resource
    private ImGroupService imGroupService;

    /**
     * 查询群列表
     */
    @GetMapping("/selectList")
    public List<ImGroupPo> selectList(@RequestParam("userId") String userId) {
        return imGroupService.selectList(userId);
    }

    /**
     * 获取群信息
     *
     * @param groupId 群id
     */
    @GetMapping("/selectOne")
    public ImGroupPo selectOne(@RequestParam("groupId") String groupId) {
        return imGroupService.selectOne(groupId);
    }

    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImGroupPo groupPo) {
        return imGroupService.insert(groupPo);
    }

    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImGroupPo groupPo) {
        return imGroupService.update(groupPo);
    }

    /**
     * 删除群
     *
     * @param groupId 群id
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("groupId") String groupId) {
        return imGroupService.deleteById(groupId);
    }

}