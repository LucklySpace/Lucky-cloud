package com.xy.lucky.database.controller;

import com.xy.lucky.database.service.ImGroupService;
import com.xy.lucky.domain.po.ImGroupPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/database/group")
@Tag(name = "ImGroup", description = "群数据库接口")
@Validated
public class ImGroupController {

    @Resource
    private ImGroupService imGroupService;

    /**
     * 查询群列表
     */
    @GetMapping("/selectList")
    @Operation(summary = "查询用户所在群列表", description = "根据用户ID查询其加入的群")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupPo.class)))
    })
    public Mono<List<ImGroupPo>> listGroupsByUser(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> imGroupService.queryList(userId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取群信息
     *
     * @param groupId 群id
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据群ID获取群信息", description = "返回群基础信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImGroupPo> getGroupById(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return Mono.fromCallable(() -> imGroupService.queryOne(groupId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 插入群信息
     *
     * @param groupPo 群信息
     */
    @PostMapping("/insert")
    @Operation(summary = "创建群", description = "新增群信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroup(@RequestBody @Valid ImGroupPo groupPo) {
        return Mono.fromCallable(() -> imGroupService.creat(groupPo)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新群信息
     *
     * @param groupPo 群信息
     */
    @PutMapping("/update")
    @Operation(summary = "更新群信息", description = "根据ID更新群基础信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateGroup(@RequestBody @Valid ImGroupPo groupPo) {
        return Mono.fromCallable(() -> imGroupService.modify(groupPo)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除群
     *
     * @param groupId 群id
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群", description = "根据ID删除群")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteGroupById(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId) {
        return Mono.fromCallable(() -> imGroupService.removeOne(groupId)).subscribeOn(Schedulers.boundedElastic());
    }

}
