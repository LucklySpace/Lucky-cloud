package com.xy.lucky.server.controller;

import com.xy.lucky.server.domain.dto.FriendDto;
import com.xy.lucky.server.domain.dto.FriendRequestDto;
import com.xy.lucky.server.domain.dto.validation.ValidationGroups;
import com.xy.lucky.server.domain.vo.FriendVo;
import com.xy.lucky.server.service.RelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/relationship", "/api/{version}/relationship"})
@Tag(name = "relationship", description = "用户关系")
public class RelationshipController {

    @Resource
    private RelationshipService relationshipService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @GetMapping("/contacts/list")
    @Operation(summary = "查询好友列表", description = "获取用户的好友列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "sequence", description = "消息序列号", required = false, in = ParameterIn.QUERY)
    })
    public Mono<?> contacts(
            @RequestParam("userId") @NotBlank(message = "用户ID不能为空") String userId,
            @RequestParam(value = "sequence", required = false) Long sequence) {
        return Mono.fromCallable(() -> relationshipService.contacts(userId, sequence))
                .subscribeOn(getScheduler());
    }

    @GetMapping("/groups/list")
    @Operation(summary = "查询群组列表", description = "获取用户加入的群组列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<?> groups(@RequestParam("userId") @NotBlank(message = "用户ID不能为空") String userId) {
        return Mono.fromCallable(() -> relationshipService.groups(userId))
                .subscribeOn(getScheduler());
    }

    @GetMapping("/newFriends/list")
    @Operation(summary = "查询好友请求列表", description = "获取收到的好友请求列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<?> newFriends(@RequestParam("userId") @NotBlank(message = "用户ID不能为空") String userId) {
        return Mono.fromCallable(() -> relationshipService.newFriends(userId))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/getFriendInfo")
    @Operation(summary = "查询好友信息", description = "获取指定好友的详细信息")
    @Parameters({
            @Parameter(name = "friendDto", description = "好友查询条件", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<FriendVo> getFriendInfo(@RequestBody @Validated(ValidationGroups.Query.class) FriendDto friendDto) {
        return Mono.fromCallable(() -> relationshipService.getFriendInfo(friendDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/search/getFriendInfoList")
    @Operation(summary = "搜索用户", description = "根据关键词搜索用户列表")
    @Parameters({
            @Parameter(name = "friendDto", description = "搜索条件", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<?> getFriendInfoList(@RequestBody FriendDto friendDto) {
        return Mono.fromCallable(() -> relationshipService.getFriendInfoList(friendDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/requestContact")
    @Operation(summary = "添加好友", description = "发送好友请求")
    @Parameters({
            @Parameter(name = "friendRequestDto", description = "好友请求信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> addFriend(@RequestBody @Validated(ValidationGroups.Create.class) FriendRequestDto friendRequestDto) {
        return Mono.fromCallable(() -> relationshipService.addFriend(friendRequestDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/approveContact")
    @Operation(summary = "审批好友请求", description = "同意或拒绝好友请求")
    @Parameters({
            @Parameter(name = "friendshipRequestDto", description = "审批信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> approveFriend(@RequestBody @Validated(ValidationGroups.Approve.class) FriendRequestDto friendshipRequestDto) {
        return Mono.fromRunnable(() -> relationshipService.approveFriend(friendshipRequestDto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/deleteFriendById")
    @Operation(summary = "删除好友", description = "删除好友关系")
    @Parameters({
            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> delFriend(@RequestBody @Validated(ValidationGroups.Delete.class) FriendDto friendDto) {
        return Mono.fromRunnable(() -> relationshipService.delFriend(friendDto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/updateFriendRemark")
    @Operation(summary = "修改好友备注", description = "修改好友的备注名称")
    @Parameters({
            @Parameter(name = "friendDto", description = "好友信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> updateFriendRemark(@RequestBody @Validated(ValidationGroups.Update.class) FriendDto friendDto) {
        return Mono.fromCallable(() -> relationshipService.updateFriendRemark(friendDto))
                .subscribeOn(getScheduler());
    }
}
