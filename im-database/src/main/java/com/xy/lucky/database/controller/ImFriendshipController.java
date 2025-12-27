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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/friend")
@Tag(name = "ImFriendShip", description = "好友关系数据库接口")
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
    public List<ImFriendshipPo> listFriendships(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imFriendshipService.queryList(ownerId, sequence);
    }

    @GetMapping("/ship/selectOne")
    @Operation(summary = "获取好友关系", description = "根据ownerId与toId查询单个好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImFriendshipPo getFriendship(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId) {
        return imFriendshipService.queryOne(ownerId, toId);
    }

    @GetMapping("/ship/selectByIds")
    @Operation(summary = "批量查询好友关系", description = "根据好友ID列表查询好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipPo.class)))
    })
    public List<ImFriendshipPo> listFriendshipsByIds(@RequestParam("ownerId") String ownerId, @RequestParam("ids") List<String> ids) {
        return imFriendshipService.queryListByIds(ownerId, ids);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建好友关系", description = "新增好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public void createFriendship(@RequestBody ImFriendshipPo friendship) {
        imFriendshipService.creat(friendship);
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友关系", description = "根据ID更新好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateFriendship(@RequestBody ImFriendshipPo friendship) {
        return imFriendshipService.modify(friendship);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除好友关系", description = "根据ownerId与friendId删除好友关系")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteFriendship(@RequestParam("ownerId") String ownerId, @RequestParam("friendId") String friendId) {
        return imFriendshipService.removeOne(ownerId, friendId);
    }

}
