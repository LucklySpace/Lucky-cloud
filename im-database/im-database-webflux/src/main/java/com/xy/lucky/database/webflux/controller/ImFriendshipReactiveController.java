package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImFriendshipReactiveService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/{version}/database/friend")
@Tag(name = "ImFriendShip", description = "好友关系数据库接口(WebFlux-R2DBC)")
@Validated
public class ImFriendshipReactiveController {

    @Resource
    private ImFriendshipReactiveService imFriendshipService;

    @GetMapping("/selectList")
    @Operation(summary = "查询好友列表", description = "根据ownerId与序列查询好友列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class)))
    })
    public Mono<List<ImFriendshipPo>> listFriendships(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                                      @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return imFriendshipService.queryList(ownerId, sequence).collectList();
    }

    @GetMapping("/ship/selectOne")
    @Operation(summary = "获取好友关系", description = "根据ownerId与toId查询单个好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImFriendshipPo> getFriendship(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                              @RequestParam("toId") @NotBlank @Size(max = 64) String toId) {
        return imFriendshipService.queryOne(ownerId, toId);
    }

    @GetMapping("/ship/selectByIds")
    @Operation(summary = "批量查询好友关系", description = "根据好友ID列表查询好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class)))
    })
    public Mono<List<ImFriendshipPo>> listFriendshipsByIds(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                                           @RequestParam("ids") @NotEmpty List<@NotBlank @Size(max = 64) String> ids) {
        return imFriendshipService.queryListByIds(ownerId, ids).collectList();
    }

    @PostMapping("/insert")
    @Operation(summary = "创建好友关系", description = "新增好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Void> createFriendship(@RequestBody @Valid ImFriendshipPo friendship) {
        return imFriendshipService.creat(friendship).then();
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友关系", description = "根据ID更新好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateFriendship(@RequestBody @Valid ImFriendshipPo friendship) {
        return imFriendshipService.modify(friendship);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除好友关系", description = "根据ownerId与friendId删除好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteFriendship(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                          @RequestParam("friendId") @NotBlank @Size(max = 64) String friendId) {
        return imFriendshipService.removeOne(ownerId, friendId);
    }
}

