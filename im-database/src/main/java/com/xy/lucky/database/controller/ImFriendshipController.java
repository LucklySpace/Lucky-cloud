package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImFriendshipService;
import com.xy.lucky.domain.po.ImFriendshipPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend")
@Tag(name = "ImFriendShip", description = "好友关系数据库接口")
public class ImFriendshipController {

    @Resource
    private ImFriendshipService imFriendshipService;

    /**
     * 查询所有好友
     */
    @GetMapping("/selectList")
    public List<ImFriendshipPo> selectList(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imFriendshipService.selectList(ownerId, sequence);
    }

    /**
     * 获取好友关系
     *
     * @param ownerId  用户ID
     * @param toId 好友id
     * @return
     */
    @GetMapping("/ship/selectOne")
    public ImFriendshipPo selectOne(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId) {
        return imFriendshipService.selectOne(ownerId, toId);
    }

    /**
     * 批量查询好友关系
     *
     * @param ownerId 用户ID
     * @param ids     好友ID列表
     * @return 好友关系列表
     */
    @GetMapping("/ship/selectByIds")
    public List<ImFriendshipPo> selectByIds(@RequestParam("ownerId") String ownerId, @RequestParam("ids") List<String> ids) {
        return imFriendshipService.selectByIds(ownerId, ids);
    }

    /**
     * 创建好友关系
     *
     * @param friendship 好友关系信息
     */
    @PostMapping("/insert")
    public void insert(@RequestBody ImFriendshipPo friendship) {
        imFriendshipService.insert(friendship);
    }

    /**
     * 更新好友关系
     *
     * @param friendship 好友关系信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImFriendshipPo friendship) {
        return imFriendshipService.update(friendship);
    }

    /**
     * 删除好友关系
     *
     * @param ownerId  用户ID
     * @param friendId 好友ID
     */
    @DeleteMapping("/delete")
    public Boolean delete(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId) {
        return imFriendshipService.delete(ownerId, friendId);
    }

}