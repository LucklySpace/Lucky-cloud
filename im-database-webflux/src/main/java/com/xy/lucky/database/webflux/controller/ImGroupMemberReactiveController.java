package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImGroupMemberReactiveService;
import com.xy.lucky.domain.po.ImGroupMemberPo;
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
@RequestMapping("/api/v1/database/group/member")
@Tag(name = "ImGroupMember", description = "群成员数据库接口(WebFlux-R2DBC)")
@Validated
public class ImGroupMemberReactiveController {

    @Resource
    private ImGroupMemberReactiveService imGroupMemberService;

    @GetMapping("/selectList")
    @Operation(summary = "查询群成员列表", description = "根据群ID查询群成员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMemberPo.class)))
    })
    public Mono<List<ImGroupMemberPo>> listGroupMembers(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return imGroupMemberService.queryList(groupId).collectList();
    }

    @GetMapping("/selectOne")
    @Operation(summary = "根据群ID与成员ID获取群成员信息", description = "返回单个群成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMemberPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImGroupMemberPo> getGroupMember(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId,
                                                @RequestParam("memberId") @NotBlank @Size(max = 64) String memberId) {
        return imGroupMemberService.queryOne(groupId, memberId);
    }

    @PostMapping("/insert")
    @Operation(summary = "添加群成员", description = "新增群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMember(@RequestBody @Valid ImGroupMemberPo groupMember) {
        return imGroupMemberService.creat(groupMember);
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量添加群成员", description = "批量新增群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMembersBatch(@RequestBody @NotEmpty List<@Valid ImGroupMemberPo> groupMemberList) {
        return imGroupMemberService.creatBatch(groupMemberList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新群成员信息", description = "根据ID更新群成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateGroupMember(@RequestBody @Valid ImGroupMemberPo groupMember) {
        return imGroupMemberService.modify(groupMember);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群成员", description = "根据ID删除群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteGroupMemberById(@RequestParam("memberId") @NotBlank @Size(max = 64) String memberId) {
        return imGroupMemberService.removeOne(memberId);
    }

    @GetMapping("/selectNinePeopleAvatar")
    @Operation(summary = "随机获取九宫格头像", description = "随机返回9个成员头像")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<List<String>> listNineAvatars(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return imGroupMemberService.queryNinePeopleAvatar(groupId);
    }
}

