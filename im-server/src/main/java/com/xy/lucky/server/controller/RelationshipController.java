package com.xy.lucky.server.controller;


import com.xy.lucky.domain.dto.FriendDto;
import com.xy.lucky.domain.dto.FriendRequestDto;
import com.xy.lucky.domain.vo.FriendVo;
import com.xy.lucky.server.service.RelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * 用户关系
 */
@Slf4j
@RestController
@RequestMapping({"/api/relationship", "/api/{version}/relationship"})
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
    public Mono contacts(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return Mono.fromCallable(() -> relationshipService.contacts(userId, sequence))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/groups/list")
    @Operation(summary = "查询群列表", tags = {"group"}, description = "请使用此接口查询群列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "sequence", description = "时序", required = true, in = ParameterIn.QUERY)
    })
    public Mono groups(@RequestParam("userId") String userId) {
        return Mono.fromCallable(() -> relationshipService.groups(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }


    @GetMapping("/newFriends/list")
    @Operation(summary = "查询好友请求列表", tags = {"friend"}, description = "请使用此接口查询好友请求列表")
    @Parameters({
            @Parameter(name = "userId", description = "请求对象", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono newFriends(@RequestParam("userId") String userId) {
        return Mono.fromCallable(() -> relationshipService.newFriends(userId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/getFriendInfo")
    @Operation(summary = "查询好友信息", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "friendDto", description = "请求对象", required = true, in = ParameterIn.QUERY),
    })
    public Mono<FriendVo> getFriendInfo(@RequestBody FriendDto friendDto) {
        return Mono.fromCallable(() -> relationshipService.getFriendInfo(friendDto))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/search/getFriendInfoList")
    @Operation(summary = "查询好友信息列表", tags = {"friend"}, description = "请使用此接口查询好友列表")
    @Parameters({
            @Parameter(name = "friendDto", description = "请求对象", required = true, in = ParameterIn.QUERY),
    })
    public Mono getFriendInfoList(@RequestBody FriendDto friendDto) {
        return Mono.fromCallable(() -> relationshipService.getFriendInfoList(friendDto))
                .subscribeOn(Schedulers.boundedElastic());
    }


    @PostMapping("/requestContact")
    @Operation(summary = "添加好友", tags = {"friend"}, description = "请使用此接口添加好友")
    @Parameters({
            @Parameter(name = "friendRequestDto", description = "添加好友", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> addFriend(@RequestBody FriendRequestDto friendRequestDto) {
        return Mono.fromCallable(() -> relationshipService.addFriend(friendRequestDto))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/approveContact")
    @Operation(summary = "同意好友申请", tags = {"friend"}, description = "请使用此接口同意好友申请")
    @Parameters({
            @Parameter(name = "friendshipRequestDto", description = "好友申请信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> approveFriend(@RequestBody FriendRequestDto friendshipRequestDto) {
        return Mono.fromRunnable(() -> relationshipService.approveFriend(friendshipRequestDto))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @PostMapping("/deleteFriendById")
    @Operation(summary = "删除好友", tags = {"friend"}, description = "请使用此接口删除好友")
    @Parameters({
            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> delFriend(@RequestBody FriendDto friendDto) {
        return Mono.fromRunnable(() -> relationshipService.delFriend(friendDto))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }


    @PostMapping("/updateFriendRemark")
    @Operation(summary = "修改好友备注", tags = {"friend"}, description = "请使用此接口修改好友备注")
    @Parameters({
            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> updateFriendRemark(@RequestBody FriendDto friendDto) {
        return Mono.fromCallable(() -> relationshipService.updateFriendRemark(friendDto))
                .subscribeOn(Schedulers.boundedElastic());
    }

}
