package com.xy.lucky.server.controller;


import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.server.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 群聊
 */
@Slf4j
@RestController
@RequestMapping({"/api/group", "/api/{version}/group"})
@Tag(name = "group", description = "群聊")
public class GroupController {

    @Resource
    private GroupService groupService;

    @PostMapping("/invite")
    @Operation(summary = "群聊邀请", tags = {"group"}, description = "请使用此接口群聊邀请")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    @Parameters({
            @Parameter(name = "groupInviteDto", description = "邀请信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> inviteGroup(@RequestBody GroupInviteDto groupInviteDto) {
        return groupService.inviteGroup(groupInviteDto);
    }

    @PostMapping("/member")
    @Operation(summary = "群成员", tags = {"group"}, description = "请使用此接口查询群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)))
    })
    @Parameters({
            @Parameter(name = "groupDto", description = "群信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Map<?, ?>> getGroupMembers(@RequestBody GroupDto groupDto) {
        return groupService.getGroupMembers(groupDto);
    }

    @PostMapping("/approve")
    @Operation(summary = "群聊邀请接受或拒绝", tags = {"group"}, description = "请使用此接口群聊邀请接受或拒绝")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    @Parameters({
            @Parameter(name = "groupInviteDto", description = "群信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> approveGroupInvite(@RequestBody GroupInviteDto groupInviteDto) {
        return groupService.approveGroupInvite(groupInviteDto);
    }

    @PostMapping("/info")
    @Operation(summary = "群信息", tags = {"group"}, description = "请使用此接口查询群信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupPo.class)))
    })
    @Parameters({
            @Parameter(name = "groupDto", description = "群信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<ImGroupPo> groupInfo(@RequestBody GroupDto groupDto) {
        return groupService.groupInfo(groupDto);
    }

    @PostMapping("/update")
    @Operation(summary = "修改群信息", tags = {"group"}, description = "请使用此接口修改群信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class)))
    })
    @Parameters({
            @Parameter(name = "groupDto", description = "群信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> updateGroupInfo(@RequestBody GroupDto groupDto) {
        return groupService.updateGroupInfo(groupDto);
    }

    @PostMapping("/quit")
    @Operation(summary = "群信息", tags = {"group"}, description = "请使用此接口退出群聊")
    @Parameters({
            @Parameter(name = "groupDto", description = "群信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> quit(@RequestBody GroupDto groupDto) {
        return groupService.quitGroup(groupDto);
    }

    @PostMapping("/member/update")
    @Operation(summary = "修改群成员信息", tags = {"group"}, description = "请使用此接口修改群成员的群昵称和备注")
    @Parameters({
            @Parameter(name = "groupMemberDto", description = "群成员信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> updateGroupMember(@RequestBody GroupMemberDto groupMemberDto) {
        return groupService.updateGroupMember(groupMemberDto);
    }
}
