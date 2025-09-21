package com.xy.server.controller;


import com.xy.domain.dto.FriendDto;
import com.xy.domain.vo.FriendVo;
import com.xy.domain.vo.FriendshipRequestVo;
import com.xy.domain.vo.GroupVo;
import com.xy.server.service.RelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    public List<FriendVo> contacts(@RequestParam("userId") String userId) {
        return relationshipService.contacts(userId);
    }

    @GetMapping("/groups/list")
    @Operation(summary = "查询群列表", tags = {"group"}, description = "请使用此接口查询群列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "sequence", description = "时序", required = true, in = ParameterIn.QUERY)
    })
    public List<GroupVo> groups(@RequestParam("userId") String userId) {
        return relationshipService.groups(userId);
    }


    @GetMapping("/newFriends/list")
    @Operation(summary = "查询好友请求列表", tags = {"friend"}, description = "请使用此接口查询好友请求列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.DEFAULT)
    })
    public List<FriendshipRequestVo> newFriends(@RequestParam("userId") String userId) {
        return relationshipService.newFriends(userId);
    }

    @PostMapping("/friendInfo")
    @Operation(summary = "查询好友信息", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "friendDto", description = "请求对象", required = true, in = ParameterIn.QUERY),
    })
    public FriendVo getFriendInfo(@RequestBody FriendDto friendDto) {
        return relationshipService.getFriendInfo(friendDto);
    }


//
//
//    @PostMapping("/add")
//    @Operation(summary = "添加好友", tags = {"friend"}, description = "请使用此接口添加好友")
//    @Parameters({
//            @Parameter(name = "friendRequestDto", description = "添加好友", required = true, in = ParameterIn.DEFAULT)
//    })
//    public void addFriend(@RequestBody FriendRequestDto friendRequestDto) {
//        friendService.addFriend(friendRequestDto);
//    }
//
//
//    @PostMapping("/del")
//    @Operation(summary = "删除好友", tags = {"friend"}, description = "请使用此接口删除好友")
//    @Parameters({
//            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
//    })
//    public void delFriend(@RequestBody FriendDto friendDto) {
//        friendService.delFriend(friendDto);
//    }
//
//
//    @PostMapping("/find")
//    @Operation(summary = "查询好友", tags = {"friend"}, description = "请使用此接口查询好友")
//    @Parameters({
//            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
//    })
//    public FriendVo findFriend(@RequestBody FriendDto friendDto) {
//        return friendService.findFriend(friendDto);
//    }
//
//
//    @GetMapping("/request")
//    @Operation(summary = "查询好友请求列表", tags = {"friend"}, description = "请使用此接口查询好友请求列表")
//    @Parameters({
//            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.DEFAULT)
//    })
//    public List<FriendshipRequestVo> request(@RequestParam("userId") String userId) {
//        return friendService.request(userId);
//    }
//
//
//    @PostMapping("/approve")
//    @Operation(summary = "同意好友申请", tags = {"friend"}, description = "请使用此接口同意好友申请")
//    @Parameters({
//            @Parameter(name = "friendshipRequestDto", description = "好友申请信息", required = true, in = ParameterIn.DEFAULT)
//    })
//    public void approveFriend(@RequestBody FriendshipRequestDto friendshipRequestDto) {
//        friendService.approveFriend(friendshipRequestDto);
//    }


}
