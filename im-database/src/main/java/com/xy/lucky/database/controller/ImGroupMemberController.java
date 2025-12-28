package com.xy.lucky.database.controller;

import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImGroupMemberService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/group/member")
@Tag(name = "ImGroupMember", description = "群成员数据库接口")
@Validated
public class ImGroupMemberController {

    @Resource
    private ImGroupMemberService imGroupMemberService;

    /**
     * 查询群成员
     *
     * @param groupId 群id
     * @return 群成员信息
     */
    @GetMapping("/selectList")
    @Operation(summary = "查询群成员列表", description = "根据群ID查询群成员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMemberPo.class)))
    })
    public Mono<List<ImGroupMemberPo>> listGroupMembers(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return Mono.fromCallable(() -> imGroupMemberService.queryList(groupId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取群成员信息
     *
     * @param groupId  群id
     * @param memberId 成员id
     * @return 群成员信息
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据群ID与成员ID获取群成员信息", description = "返回单个群成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMemberPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImGroupMemberPo> getGroupMember(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId, @RequestParam("memberId") @NotBlank @Size(max = 64) String memberId) {
        return Mono.fromCallable(() -> imGroupMemberService.queryOne(groupId, memberId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 插入群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否插入成功
     */
    @PostMapping("/insert")
    @Operation(summary = "添加群成员", description = "新增群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMember(@RequestBody @Valid ImGroupMemberPo groupMember) {
        return Mono.fromCallable(() -> imGroupMemberService.creat(groupMember)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 批量插入群成员
     *
     * @param groupMemberList 群成员信息
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量添加群成员", description = "批量新增群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMembersBatch(@RequestBody @NotEmpty List<@Valid ImGroupMemberPo> groupMemberList) {
        return Mono.fromCallable(() -> imGroupMemberService.creatBatch(groupMemberList)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新群成员信息
     *
     * @param groupMember 群成员信息
     * @return 是否更新成功
     */
    @PutMapping("/update")
    @Operation(summary = "更新群成员信息", description = "根据ID更新群成员信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateGroupMember(@RequestBody @Valid ImGroupMemberPo groupMember) {
        return Mono.fromCallable(() -> imGroupMemberService.modify(groupMember)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除群成员信息
     *
     * @param memberId 群成员id
     * @return 是否删除成功
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群成员", description = "根据ID删除群成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteGroupMemberById(@RequestParam("memberId") @NotBlank @Size(max = 64) String memberId) {
        return Mono.fromCallable(() -> imGroupMemberService.removeOne(memberId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 随机获取9个用户头像，用于生成九宫格头像
     *
     * @param groupId
     * @return
     */
    @GetMapping("/selectNinePeopleAvatar")
    @Operation(summary = "随机获取九宫格头像", description = "随机返回9个成员头像")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<List<String>> listNineAvatars(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return Mono.fromCallable(() -> imGroupMemberService.queryNinePeopleAvatar(groupId)).subscribeOn(Schedulers.boundedElastic());
    }

}
