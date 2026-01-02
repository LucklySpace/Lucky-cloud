package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImFriendshipRequestReactiveService;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/database/friend/request")
@Tag(name = "ImFriendShipRequest", description = "好友请求数据库接口")
@RequiredArgsConstructor
public class ImFriendshipRequestReactiveController {

    private final ImFriendshipRequestReactiveService service;

    @GetMapping("/selectList")
    @Operation(summary = "查询好友请求列表")
    public Flux<ImFriendshipRequestPo> listFriendshipRequests(@RequestParam("userId") String userId) {
        return service.queryList(userId);
    }

    @PostMapping("/selectOne")
    @Operation(summary = "获取单个好友请求")
    public Mono<ImFriendshipRequestPo> getFriendshipRequest(@RequestBody ImFriendshipRequestPo requestPo) {
        return service.queryOne(requestPo);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建好友请求")
    public Mono<Boolean> createFriendshipRequest(@RequestBody ImFriendshipRequestPo requestPo) {
        return service.creat(requestPo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友请求")
    public Mono<Boolean> updateFriendshipRequest(@RequestBody ImFriendshipRequestPo requestPo) {
        return service.modify(requestPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除好友请求")
    public Mono<Boolean> deleteFriendshipRequestById(@RequestParam("requestId") String requestId) {
        return service.removeOne(requestId);
    }
}
