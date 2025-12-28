package com.xy.lucky.database.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImFriendshipGroupService;
import com.xy.lucky.domain.po.ImFriendshipGroupPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend/group")
@Tag(name = "ImFriendshipGroup", description = "好友分组数据库接口")
@Validated
public class ImFriendshipGroupController {

    @Resource
    private ImFriendshipGroupService imFriendshipGroupService;

    @GetMapping("/selectList")
    @Operation(summary = "查询好友分组列表", description = "根据ownerId查询其好友分组列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipGroupPo.class)))
    })
    public Mono<List<ImFriendshipGroupPo>> listFriendshipGroups(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId) {
        QueryWrapper<ImFriendshipGroupPo> query = new QueryWrapper<>();
        query.eq("owner_id", ownerId);
        return Mono.fromCallable(() -> imFriendshipGroupService.list(query)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/selectOne")
    @Operation(summary = "获取单个好友分组", description = "根据分组ID获取好友分组信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipGroupPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImFriendshipGroupPo> getFriendshipGroup(@RequestParam("id") @NotBlank @Size(max = 64) String id) {
        return Mono.fromCallable(() -> imFriendshipGroupService.getById(id)).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/insert")
    @Operation(summary = "创建好友分组", description = "新增好友分组")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createFriendshipGroup(@RequestBody @Valid ImFriendshipGroupPo friendshipGroupPo) {
        return Mono.fromCallable(() -> imFriendshipGroupService.save(friendshipGroupPo)).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建好友分组", description = "批量新增好友分组")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createFriendshipGroupsBatch(@RequestBody @NotEmpty List<@Valid ImFriendshipGroupPo> friendshipGroupPoList) {
        return Mono.fromCallable(() -> imFriendshipGroupService.saveBatch(friendshipGroupPoList)).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友分组", description = "根据ID更新好友分组信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateFriendshipGroup(@RequestBody @Valid ImFriendshipGroupPo friendshipGroupPo) {
        return Mono.fromCallable(() -> imFriendshipGroupService.updateById(friendshipGroupPo)).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除好友分组", description = "根据ID删除好友分组")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteFriendshipGroupById(@RequestParam("id") @NotBlank @Size(max = 64) String id) {
        return Mono.fromCallable(() -> imFriendshipGroupService.removeById(id)).subscribeOn(Schedulers.boundedElastic());
    }
}
