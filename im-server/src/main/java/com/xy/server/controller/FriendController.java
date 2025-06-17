package com.xy.server.controller;


import com.xy.domain.vo.FriendVo;
import com.xy.server.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/friend")
@Tag(name = "friend", description = "用户好友")
public class FriendController {
    @Resource
    private FriendService friendService;

    @GetMapping("/list")
    @Operation(summary = "查询好友列表", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "sequence", description = "时序", required = true, in = ParameterIn.QUERY)
    })
    public List<FriendVo> list(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return friendService.list(userId, sequence);
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
