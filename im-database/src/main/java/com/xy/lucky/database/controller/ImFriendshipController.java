package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImFriendshipService;
import com.xy.lucky.domain.po.ImFriendshipPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/v1/database/friend")
@Tag(name = "ImFriendShip", description = "好友关系数据库接口")
@Validated
public class ImFriendshipController {

    @Resource
    private ImFriendshipService imFriendshipService;

    @GetMapping("/selectList")
    @Operation(summary = "查询好友列表", description = "根据ownerId与序列查询好友列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class)))
    })
    public Mono<List<ImFriendshipPo>> listFriendships(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId, @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return Mono.fromCallable(() -> imFriendshipService.queryList(ownerId, sequence)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/ship/selectOne")
    @Operation(summary = "获取好友关系", description = "根据ownerId与toId查询单个好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImFriendshipPo> getFriendship(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId, @RequestParam("toId") @NotBlank @Size(max = 64) String toId) {
        return Mono.fromCallable(() -> imFriendshipService.queryOne(ownerId, toId)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/ship/selectByIds")
    @Operation(summary = "批量查询好友关系", description = "根据好友ID列表查询好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class)))
    })
    public Mono<List<ImFriendshipPo>> listFriendshipsByIds(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId, @RequestParam("ids") @NotEmpty List<@NotBlank @Size(max = 64) String> ids) {
        return Mono.fromCallable(() -> imFriendshipService.queryListByIds(ownerId, ids)).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/insert")
    @Operation(summary = "创建好友关系", description = "新增好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Void> createFriendship(@RequestBody @Valid ImFriendshipPo friendship) {
        return Mono.fromRunnable(() -> imFriendshipService.creat(friendship)).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友关系", description = "根据ID更新好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateFriendship(@RequestBody @Valid ImFriendshipPo friendship) {
        return Mono.fromCallable(() -> imFriendshipService.modify(friendship)).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除好友关系", description = "根据ownerId与friendId删除好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteFriendship(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId, @RequestParam("friendId") @NotBlank @Size(max = 64) String friendId) {
        return Mono.fromCallable(() -> imFriendshipService.removeOne(ownerId, friendId)).subscribeOn(Schedulers.boundedElastic());
    }

}
