package com.xy.lucky.server.controller;

import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.server.domain.dto.GroupDto;
import com.xy.lucky.server.domain.dto.GroupInviteDto;
import com.xy.lucky.server.domain.dto.GroupMemberDto;
import com.xy.lucky.server.domain.dto.validation.ValidationGroups;
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
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/group", "/api/{version}/group"})
@Tag(name = "group", description = "群聊")
public class GroupController {

    @Resource
    private GroupService groupService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @PostMapping("/invite")
    @Operation(summary = "群邀请", description = "创建群聊或邀请用户入群")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    })
    @Parameters({
            @Parameter(name = "groupInviteDto", description = "邀请信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> inviteGroup(@RequestBody @Valid GroupInviteDto groupInviteDto) {
        return Mono.fromCallable(() -> groupService.inviteGroup(groupInviteDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/member")
    @Operation(summary = "查询群成员", description = "获取群组的所有成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    @Parameters({
            @Parameter(name = "groupDto", description = "群组信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<?> getGroupMembers(@RequestBody @Validated(ValidationGroups.Query.class) GroupDto groupDto) {
        return Mono.fromCallable(() -> groupService.getGroupMembers(groupDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/approve")
    @Operation(summary = "审批群邀请", description = "接受或拒绝群聊邀请")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class)))
    })
    @Parameters({
            @Parameter(name = "groupInviteDto", description = "审批信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<String> approveGroupInvite(@RequestBody @Validated(ValidationGroups.Approve.class) GroupInviteDto groupInviteDto) {
        return Mono.fromCallable(() -> groupService.approveGroupInvite(groupInviteDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/info")
    @Operation(summary = "查询群信息", description = "获取群组详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImGroupPo.class)))
    })
    @Parameters({
            @Parameter(name = "groupDto", description = "群组信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<ImGroupPo> groupInfo(@RequestBody @Validated(ValidationGroups.Query.class) GroupDto groupDto) {
        return Mono.fromCallable(() -> groupService.groupInfo(groupDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/update")
    @Operation(summary = "修改群信息", description = "修改群组基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class)))
    })
    @Parameters({
            @Parameter(name = "groupDto", description = "群组信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> updateGroupInfo(@RequestBody @Validated(ValidationGroups.Update.class) GroupDto groupDto) {
        return Mono.fromCallable(() -> groupService.updateGroupInfo(groupDto))
                .subscribeOn(getScheduler());
    }

    @PostMapping("/quit")
    @Operation(summary = "退出群聊", description = "用户退出群聊（群主不可退出）")
    @Parameters({
            @Parameter(name = "groupDto", description = "群组信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Void> quit(@RequestBody @Validated(ValidationGroups.Delete.class) GroupDto groupDto) {
        return Mono.fromRunnable(() -> groupService.quitGroup(groupDto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/member/update")
    @Operation(summary = "修改群成员信息", description = "修改群成员的群昵称和备注")
    @Parameters({
            @Parameter(name = "groupMemberDto", description = "群成员信息", required = true, in = ParameterIn.DEFAULT)
    })
    public Mono<Boolean> updateGroupMember(@RequestBody @Valid GroupMemberDto groupMemberDto) {
        return Mono.fromCallable(() -> groupService.updateGroupMember(groupMemberDto))
                .subscribeOn(getScheduler());
    }

    // ==================== 群管理接口 ====================

    @PostMapping("/member/kick")
    @Operation(summary = "踢出群成员", description = "群主或管理员可踢出普通成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> kickMember(@RequestBody @Validated(ValidationGroups.KickMember.class) GroupMemberDto dto) {
        return Mono.fromRunnable(() -> groupService.kickMember(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/member/setAdmin")
    @Operation(summary = "设置/取消管理员", description = "群主可设置或取消成员的管理员身份")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> setAdmin(@RequestBody @Validated(ValidationGroups.SetAdmin.class) GroupMemberDto dto) {
        return Mono.fromRunnable(() -> groupService.setAdmin(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/transferOwner")
    @Operation(summary = "移交群主", description = "群主可将群主身份移交给其他成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> transferOwner(@RequestBody @Validated(ValidationGroups.TransferOwner.class) GroupMemberDto dto) {
        return Mono.fromRunnable(() -> groupService.transferOwner(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/setJoinMode")
    @Operation(summary = "设置群加入方式", description = "管理员可设置群的加入方式（禁止/审批/自由）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> setJoinMode(@RequestBody @Validated(ValidationGroups.SetJoinMode.class) GroupDto dto) {
        return Mono.fromRunnable(() -> groupService.setJoinMode(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/member/mute")
    @Operation(summary = "禁言/取消禁言成员", description = "管理员可禁言或取消禁言普通成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> muteMember(@RequestBody @Validated(ValidationGroups.MuteMember.class) GroupMemberDto dto) {
        return Mono.fromRunnable(() -> groupService.muteMember(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/muteAll")
    @Operation(summary = "全员禁言/取消全员禁言", description = "管理员可开启或关闭全员禁言")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> muteAll(@RequestBody @Validated(ValidationGroups.MuteAll.class) GroupDto dto) {
        return Mono.fromRunnable(() -> groupService.muteAll(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/dismiss")
    @Operation(summary = "解散群组", description = "群主可解散群组")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> dismissGroup(@RequestBody @Validated(ValidationGroups.DismissGroup.class) GroupDto dto) {
        return Mono.fromRunnable(() -> groupService.dismissGroup(dto))
                .subscribeOn(getScheduler())
                .then();
    }

    @PostMapping("/announcement")
    @Operation(summary = "设置群公告", description = "管理员可设置群公告")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Void> setAnnouncement(@RequestBody @Validated(ValidationGroups.Update.class) GroupDto dto) {
        return Mono.fromRunnable(() -> groupService.setAnnouncement(dto))
                .subscribeOn(getScheduler())
                .then();
    }
}
