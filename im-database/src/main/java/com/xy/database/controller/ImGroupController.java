package com.xy.database.controller;

import com.xy.database.service.ImGroupService;
import com.xy.domain.po.ImGroupPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/group")
@Tag(name = "ImGroup", description = "群数据库接口")
@RequiredArgsConstructor
public class ImGroupController {

    private final ImGroupService imGroupService;

    /**
     * 查询所有好友
     */
    @GetMapping("/list")
    public List<ImGroupPo> list(@RequestParam("userId") String userId) {
        return imGroupService.list(userId);
    }

    /**
     * 获取群信息
     *
     * @param groupId 成员id
     */
    @GetMapping("/getOne")
    public ImGroupPo getOne(@RequestParam("groupId") String groupId) {
        return imGroupService.getById(groupId);
    }


    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImGroupPo groupPo) {
        return imGroupService.save(groupPo);
    }

    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     */
    @PutMapping("/updateById")
    public Boolean updateById(@RequestBody ImGroupPo groupPo) {
        return imGroupService.updateById(groupPo);
    }

    /**
     * 删除群
     *
     * @param groupId 群id
     */
    @DeleteMapping("/{groupId}")
    public Boolean deleteById(@PathVariable String groupId) {
        return imGroupService.removeById(groupId);
    }


}
