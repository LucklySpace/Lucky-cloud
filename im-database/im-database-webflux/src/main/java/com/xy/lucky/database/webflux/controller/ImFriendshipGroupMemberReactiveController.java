package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImFriendshipGroupMemberReactiveService;
import com.xy.lucky.domain.po.ImFriendshipGroupMemberPo;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/{version}/database/friend/group/member")
@Tag(name = "ImFriendshipGroupMember", description = "好友分组成员数据库接口(WebFlux-R2DBC)")
@Validated
public class ImFriendshipGroupMemberReactiveController {

    @Resource
    private ImFriendshipGroupMemberReactiveService imFriendshipGroupMemberService;

    @GetMapping("/selectList")
    @Operation(summary = "查询好友分组成员列表", description = "根据分组ID查询成员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipGroupMemberPo.class)))
    })
    public Mono<List<ImFriendshipGroupMemberPo>> listFriendshipGroupMembers(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return imFriendshipGroupMemberService.queryList(groupId).collectList();
    }

    @GetMapping("/selectOne")
    @Operation(summary = "获取好友分组成员", description = "根据分组ID与成员ID获取成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImFriendshipGroupMemberPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImFriendshipGroupMemberPo> getFriendshipGroupMember(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId,
                                                                    @RequestParam("memberId") @NotBlank @Size(max = 64) String memberId) {
        return imFriendshipGroupMemberService.queryOne(groupId, memberId);
    }

    @PostMapping("/insert")
    @Operation(summary = "添加好友分组成员", description = "新增分组成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createFriendshipGroupMember(@RequestBody @Valid ImFriendshipGroupMemberPo memberPo) {
        return imFriendshipGroupMemberService.create(memberPo);
    }


    @PostMapping("/batchInsert")
    @Operation(summary = "批量添加好友分组成员", description = "批量新增分组成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createFriendshipGroupMembersBatch(@RequestBody @NotEmpty List<@Valid ImFriendshipGroupMemberPo> memberPoList) {
        return imFriendshipGroupMemberService.createBatch(memberPoList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新好友分组成员", description = "根据ID更新分组成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateFriendshipGroupMember(@RequestBody @Valid ImFriendshipGroupMemberPo memberPo) {
        return imFriendshipGroupMemberService.modify(memberPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除好友分组成员", description = "根据成员ID删除分组成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteFriendshipGroupMemberById(@RequestParam("memberId") @NotBlank @Size(max = 64) String memberId) {
        return imFriendshipGroupMemberService.removeOne(memberId);
    }
}

