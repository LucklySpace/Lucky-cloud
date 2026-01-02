package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImGroupInviteRequestReactiveService;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/database/group/invite/request")
@Tag(name = "ImGroupInviteRequest", description = "群邀请请求数据库接口")
@RequiredArgsConstructor
public class ImGroupInviteRequestReactiveController {

    private final ImGroupInviteRequestReactiveService service;

    @GetMapping("/selectList")
    @Operation(summary = "查询群邀请请求列表")
    public Flux<ImGroupInviteRequestPo> listRequests(@RequestParam("userId") String userId) {
        return service.queryList(userId);
    }

    @PostMapping("/selectOne")
    @Operation(summary = "获取单个群邀请请求")
    public Mono<ImGroupInviteRequestPo> getRequest(@RequestBody ImGroupInviteRequestPo po) {
        return service.queryOne(po);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建群邀请请求")
    public Mono<Boolean> createRequest(@RequestBody ImGroupInviteRequestPo po) {
        return service.creat(po);
    }

    @PutMapping("/update")
    @Operation(summary = "更新群邀请请求")
    public Mono<Boolean> updateRequest(@RequestBody ImGroupInviteRequestPo po) {
        return service.modify(po);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群邀请请求")
    public Mono<Boolean> deleteRequest(@RequestParam("requestId") String requestId) {
        return service.removeOne(requestId);
    }
}
