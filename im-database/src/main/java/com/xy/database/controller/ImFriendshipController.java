package com.xy.database.controller;


import com.xy.database.security.SecurityInner;
import com.xy.database.service.ImFriendshipService;
import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend")
@Tag(name = "ImFriendShip", description = "好友关系数据库接口")
@RequiredArgsConstructor
public class ImFriendshipController {

    private final ImFriendshipService imFriendshipService;

    /**
     * 查询所有好友
     */
    @GetMapping("/list")
    public List<ImFriendshipPo> list(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imFriendshipService.list(ownerId, sequence);
    }


    /**
     * 获取好友关系
     *
     * @param ownerId  用户ID
     * @param toId 好友id
     * @return
     */
    @GetMapping("/ship/getOne")
    public ImFriendshipPo getOne(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId) {
        return imFriendshipService.getOne(ownerId, toId);
    }

    /**
     * 批量查询好友关系
     *
     * @param ownerId 用户ID
     * @param ids     好友ID列表
     * @return 好友关系列表
     */
    @GetMapping("/ship/list")
    public List<ImFriendshipPo> shipList(@RequestParam("ownerId") String ownerId, @RequestParam("ids") List<String> ids) {
        return imFriendshipService.getFriendshipList(ownerId, ids);
    }

    /**
     * 添加好友请求
     *
     * @param request 好友请求信息
     */
    @PostMapping("/request/add")
    public void addFriendRequest(@RequestBody ImFriendshipRequestPo request) {
        request.setId(UUID.randomUUID().toString());
        imFriendshipService.saveFriendRequest(request);
    }

    @PostMapping("/request/update")
    public void updateFriendRequest(@RequestBody ImFriendshipRequestPo request) {
        imFriendshipService.updateFriendRequest(request);
    }

    /**
     * 更新好友请求状态
     *
     * @param requestId 请求ID
     * @param status    审批状态
     */
    @PutMapping("/request/updateStatus")
    public void updateFriendRequestStatus(@RequestParam("requestId") String requestId, @RequestParam("status") Integer status) {
        imFriendshipService.updateFriendRequestStatus(requestId, status);
    }

    /**
     * 创建好友关系
     *
     * @param friendship 好友关系信息
     */
    @PostMapping("/create")
    public void createFriendship(@RequestBody ImFriendshipPo friendship) {
        imFriendshipService.saveFriendship(friendship);
    }

    /**
     * 删除好友关系
     *
     * @param ownerId  用户ID
     * @param friendId 好友ID
     */
    @PostMapping("/delete")
    public Boolean deleteFriendship(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId) {
        return imFriendshipService.deleteFriendship(ownerId, friendId);
    }
}