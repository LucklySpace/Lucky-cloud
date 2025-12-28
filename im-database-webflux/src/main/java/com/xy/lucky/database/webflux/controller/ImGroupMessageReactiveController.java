package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImGroupMessageReactiveService;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
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
@RequestMapping("/api/{version}/database/group/message")
@Tag(name = "ImGroup", description = "群聊消息数据库接口(WebFlux-R2DBC)")
@Validated
public class ImGroupMessageReactiveController {

    @Resource
    private ImGroupMessageReactiveService imGroupMessageService;

    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取群聊消息", description = "返回指定消息ID的群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMessagePo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImGroupMessagePo> getGroupMessageById(@RequestParam("messageId") @NotBlank @Size(max = 64) String messageId) {
        return imGroupMessageService.queryOne(messageId);
    }

    @GetMapping("/selectList")
    @Operation(summary = "获取用户群聊消息列表", description = "按用户ID与序列获取群聊消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMessagePo.class)))
    })
    public Mono<List<ImGroupMessagePo>> listGroupMessages(@RequestParam("userId") @NotBlank @Size(max = 64) String userId, @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return imGroupMessageService.queryList(userId, sequence).collectList();
    }

    @PostMapping("/insert")
    @Operation(summary = "创建群聊消息", description = "新增群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMessage(@RequestBody @Valid ImGroupMessagePo messagePo) {
        return imGroupMessageService.creat(messagePo);
    }

    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建群聊消息状态", description = "批量新增群聊消息阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMessageStatusBatch(@RequestBody @NotEmpty List<@Valid ImGroupMessageStatusPo> messagePoList) {
        return imGroupMessageService.creatBatch(messagePoList);
    }

    @PutMapping("/update")
    @Operation(summary = "更新群聊消息", description = "根据ID更新群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateGroupMessage(@RequestBody @Valid ImGroupMessagePo messagePo) {
        return imGroupMessageService.modify(messagePo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群聊消息", description = "根据ID删除群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteGroupMessageById(@RequestParam("messageId") @NotBlank @Size(max = 64) String messageId) {
        return imGroupMessageService.removeOne(messageId);
    }

    @GetMapping("/last")
    @Operation(summary = "获取最后一条群聊消息", description = "按群ID与用户ID查询最新群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMessagePo.class)))
    })
    public Mono<ImGroupMessagePo> getLastGroupMessage(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId, @RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return imGroupMessageService.queryLast(groupId, userId);
    }

    @GetMapping("/selectReadStatus")
    @Operation(summary = "查询群聊消息阅读状态", description = "按群ID、接收者ID与状态码统计阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<Integer> getGroupMessageReadStatus(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId, @RequestParam("toId") @NotBlank @Size(max = 64) String toId, @RequestParam("code") @NotNull @Min(0) Integer code) {
        return imGroupMessageService.queryReadStatus(groupId, toId, code);
    }
}
