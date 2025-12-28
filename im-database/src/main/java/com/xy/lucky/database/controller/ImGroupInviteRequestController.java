package com.xy.lucky.database.controller;

import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImGroupInviteRequestService;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/v1/database/group/invite")
@Tag(name = "ImFriendShip", description = "群邀请请求接口")
public class ImGroupInviteRequestController {

    @Resource
    private ImGroupInviteRequestService imGroupInviteRequestService;

    /**
     * 获取群邀请请求
     *
     * @param requestId
     * @return
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取群邀请请求", description = "返回指定ID的群邀请请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupInviteRequestPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImGroupInviteRequestPo> getGroupInviteRequestById(@RequestParam("requestId") String requestId) {
        return Mono.fromCallable(() -> imGroupInviteRequestService.getById(requestId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 保存或更新群邀请请求
     *
     * @param requestPo
     * @return
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增或更新群邀请请求", description = "存在则更新，不存在则新增")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Boolean> upsertGroupInviteRequest(@RequestBody ImGroupInviteRequestPo requestPo) {
        return Mono.fromCallable(() -> imGroupInviteRequestService.saveOrUpdate(requestPo)).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/saveOrUpdate/batch")
    @Operation(summary = "批量新增或更新群邀请请求", description = "批量新增或更新群邀请请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Mono<Boolean> upsertGroupInviteRequestsBatch(@RequestBody List<ImGroupInviteRequestPo> requestPoList) {
        return Mono.fromCallable(() -> imGroupInviteRequestService.saveOrUpdateBatch(requestPoList)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除群邀请请求
     *
     * @param requestId
     * @return
     */
    @GetMapping("/deleteById")
    @Operation(summary = "删除群邀请请求", description = "根据ID删除群邀请请求")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteGroupInviteRequestById(@RequestParam("requestId") String requestId) {
        return Mono.fromCallable(() -> imGroupInviteRequestService.removeOne(requestId)).subscribeOn(Schedulers.boundedElastic());
    }

}
