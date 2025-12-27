package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImFriendshipRequestService;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @GetMapping("/selectList")
    @Operation(summary = "查询好友请求列表", description = "根据用户ID查询收到的好友请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipRequestPo.class)))
    })
    public List<ImFriendshipRequestPo> listFriendshipRequests(@RequestParam("userId") String userId) {
        return imFriendshipRequestService.queryList(userId);
    }

    @PostMapping("/selectOne")
    @Operation(summary = "获取单个好友请求", description = "根据请求内容查询单个好友请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipRequestPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImFriendshipRequestPo getFriendshipRequest(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.queryOne(requestPo);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建好友请求", description = "新增一条好友请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createFriendshipRequest(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.creat(requestPo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友请求", description = "根据ID更新好友请求状态或内容")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateFriendshipRequest(@RequestBody ImFriendshipRequestPo requestPo) {
        return imFriendshipRequestService.modify(requestPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除好友请求", description = "根据请求ID删除好友请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteFriendshipRequestById(@RequestParam("requestId") String requestId) {
        return imFriendshipRequestService.removeOne(requestId);
    }

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
