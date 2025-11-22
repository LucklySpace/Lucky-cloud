package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImFriendshipRequestService;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend/request")
@Tag(name = "ImFriendShipRequest", description = "好友关系数据库接口")
public class ImFriendShipRequestController {

    @Resource
    private ImFriendshipRequestService imFriendshipRequestService;

    /**
     * 查询添加好友请求
     *
     * @param userId
     * @return
     */
    @GetMapping("/selectList")
    public List<ImFriendshipRequestPo> selectList(@RequestParam("userId") String userId) {
        return imFriendshipRequestService.selectList(userId);
    }

    /**
     * 获取好友请求
     *
     * @param requestPo 请求
     * @return
     */
    @PostMapping("/selectOne")
    public ImFriendshipRequestPo selectOne(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.selectOne(requestPo);
    }
    
    /**
     * 插入好友请求
     *
     * @param requestPo 好友请求
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    public Boolean insert(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.insert(requestPo);
    }

    /**
     * 更新好友请求
     *
     * @param requestPo 好友请求
     * @return 是否更新成功
     */
    @PutMapping("/update")
    public Boolean update(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.update(requestPo);
    }
    
    /**
     * 删除好友请求
     *
     * @param requestId 请求ID
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    public Boolean deleteById(@RequestParam("requestId") String requestId) {
        return imFriendshipRequestService.deleteById(requestId);
    }


//
//    /**
//     * 添加好友请求
//     *
//     * @param request 好友请求信息
//     */
//    @PostMapping("/request/add")
//    public void addFriendRequest(@RequestBody ImFriendshipRequestPo request) {
//        request.setId(UUID.randomUUID().toString());
//        imFriendshipService.insert(request);
//    }
//
//    @PostMapping("/request/update")
//    public void updateFriendRequest(@RequestBody ImFriendshipRequestPo request) {
//        imFriendshipService.updateFriendRequest(request);
//    }
//
//    /**
//     * 更新好友请求状态
//     *
//     * @param requestId 请求ID
//     * @param status    审批状态
//     */
//    @PutMapping("/request/updateStatus")
//    public void updateFriendRequestStatus(@RequestParam("requestId") String requestId, @RequestParam("status") Integer status) {
//        imFriendshipService.updateFriendRequestStatus(requestId, status);
//    }

}