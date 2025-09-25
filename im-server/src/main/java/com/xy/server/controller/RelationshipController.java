package com.xy.server.controller;


import com.xy.domain.dto.FriendDto;
import com.xy.domain.dto.FriendRequestDto;
import com.xy.general.response.domain.Result;
import com.xy.server.service.RelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


/**
 * 用户关系
 */
@Slf4j
@RestController
@RequestMapping("/api/{version}/relationship")
@Tag(name = "relationship", description = "用户关系")
public class RelationshipController {

    @Resource
    private RelationshipService relationshipService;

    @GetMapping("/contacts/list")
    @Operation(summary = "查询好友列表", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "sequence", description = "时序", required = true, in = ParameterIn.QUERY)
    })
    public Result contacts(@RequestParam("userId") String userId) {
        return relationshipService.contacts(userId);
    }

    @GetMapping("/groups/list")
    @Operation(summary = "查询群列表", tags = {"group"}, description = "请使用此接口查询群列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "sequence", description = "时序", required = true, in = ParameterIn.QUERY)
    })
    public Result groups(@RequestParam("userId") String userId) {
        return relationshipService.groups(userId);
    }


    @GetMapping("/newFriends/list")
    @Operation(summary = "查询好友请求列表", tags = {"friend"}, description = "请使用此接口查询好友请求列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Result newFriends(@RequestParam("userId") String userId) {
        return relationshipService.newFriends(userId);
    }

    @PostMapping("/getFriendInfo")
    @Operation(summary = "查询好友信息", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "friendDto", description = "请求对象", required = true, in = ParameterIn.QUERY),
    })
    public Result getFriendInfo(@RequestBody FriendDto friendDto) {
        return relationshipService.getFriendInfo(friendDto);
    }

    @PostMapping("/search/getFriendInfoList")
    @Operation(summary = "查询好友信息列表", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "friendDto", description = "请求对象", required = true, in = ParameterIn.QUERY),
    })
    public Result getFriendInfoList(@RequestBody FriendDto friendDto) {
        return relationshipService.getFriendInfoList(friendDto);
    }


    @PostMapping("/requestContact")
    @Operation(summary = "添加好友", tags = {"friend"}, description = "请使用此接口添加好友")
    @Parameters({
            @Parameter(name = "friendRequestDto", description = "添加好友", required = true, in = ParameterIn.DEFAULT)
    })
    public Result addFriend(@RequestBody FriendRequestDto friendRequestDto) {
        return relationshipService.addFriend(friendRequestDto);
    }

    @PostMapping("/approveContact")
    @Operation(summary = "同意好友申请", tags = {"friend"}, description = "请使用此接口同意好友申请")
    @Parameters({
            @Parameter(name = "friendshipRequestDto", description = "好友申请信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Result approveFriend(@RequestBody FriendRequestDto friendshipRequestDto) {
        return relationshipService.approveFriend(friendshipRequestDto);
    }

    @PostMapping("/deleteFriendById")
    @Operation(summary = "删除好友", tags = {"friend"}, description = "请使用此接口删除好友")
    @Parameters({
            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Result delFriend(@RequestBody FriendDto friendDto) {
        return relationshipService.delFriend(friendDto);
    }

}
