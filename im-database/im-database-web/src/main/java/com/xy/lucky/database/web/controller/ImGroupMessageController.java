package com.xy.lucky.database.web.controller;

import com.xy.lucky.database.web.security.SecurityInner;
import com.xy.lucky.database.web.service.ImGroupMessageService;
import com.xy.lucky.database.web.service.ImGroupMessageStatusService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@SecurityInner
@RestController
@RequestMapping("/api/{version}/database/group/message")
@Tag(name = "ImGroup", description = "群聊消息数据库接口")
@Validated
public class ImGroupMessageController {

    @Resource
    private ImGroupMessageService imGroupMessageService;

    @Resource
    private ImGroupMessageStatusService imGroupMessageStatusService;

    /**
     * 获取群聊消息
     *
     * @param messageId 消息id
     * @return 群聊消息
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取群聊消息", description = "返回指定消息ID的群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMessagePo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImGroupMessagePo> getGroupMessageById(@RequestParam("messageId") @NotBlank @Size(max = 64) String messageId) {
        return Mono.fromCallable(() -> imGroupMessageService.queryOne(messageId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取用户群聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/selectList")
    @Operation(summary = "获取用户群聊消息列表", description = "按用户ID与序列获取群聊消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMessagePo.class)))
    })
    public Mono<List<ImGroupMessagePo>> listGroupMessages(@RequestParam("userId") @NotBlank @Size(max = 64) String userId, @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return Mono.fromCallable(() -> imGroupMessageService.queryList(userId, sequence)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 插入群聊消息
     *
     * @param messagePo 群聊消息
     */
    @PostMapping("/insert")
    @Operation(summary = "创建群聊消息", description = "新增群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMessage(@RequestBody @Valid ImGroupMessagePo messagePo) {
        return Mono.fromCallable(() -> imGroupMessageService.creat(messagePo)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 批量插入群聊消息
     *
     * @param messagePoList 群聊消息列表
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建群聊消息状态", description = "批量新增群聊消息阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createGroupMessageStatusBatch(@RequestBody @NotEmpty List<@Valid ImGroupMessageStatusPo> messagePoList) {
        return Mono.fromCallable(() -> imGroupMessageService.creatBatch(messagePoList)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 更新群聊消息
     *
     * @param messagePo 群聊消息
     */
    @PutMapping("/update")
    @Operation(summary = "更新群聊消息", description = "根据ID更新群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateGroupMessage(@RequestBody @Valid ImGroupMessagePo messagePo) {
        return Mono.fromCallable(() -> imGroupMessageService.modify(messagePo)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 删除群聊消息
     *
     * @param messageId 消息ID
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除群聊消息", description = "根据ID删除群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteGroupMessageById(@RequestParam("messageId") @NotBlank @Size(max = 64) String messageId) {
        return Mono.fromCallable(() -> imGroupMessageService.removeOne(messageId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取最后一条消息
     *
     * @param groupId 群聊id
     * @param userId  用户id
     * @return 私聊消息
     */
    @GetMapping("/last")
    @Operation(summary = "获取最后一条群聊消息", description = "按群ID与用户ID查询最新群聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImGroupMessagePo.class)))
    })
    public Mono<ImGroupMessagePo> getLastGroupMessage(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId, @RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> imGroupMessageService.queryLast(groupId, userId)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 查询阅读状态
     *
     * @param groupId 群id
     * @param toId    接受者id
     * @param code    阅读状态码
     * @return 未/已读消息数
     */
    @GetMapping("/selectReadStatus")
    @Operation(summary = "查询群聊消息阅读状态", description = "按群ID、接收者ID与状态码统计阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<Integer> getGroupMessageReadStatus(@RequestParam("groupId") @NotBlank @Size(max = 64) String groupId, @RequestParam("toId") @NotBlank @Size(max = 64) String toId, @RequestParam("code") @NotNull @Min(0) Integer code) {
        return Mono.fromCallable(() -> imGroupMessageService.queryReadStatus(groupId, toId, code)).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 插入群聊消息状态
     *
     * @param groupReadStatusList 群聊消息群成员阅读状态
     */
    @PostMapping("/status/batch/insert")
    @Operation(summary = "批量插入群聊消息阅读状态", description = "批量保存群聊消息阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> insertGroupMessageStatusBatch(@RequestBody @NotEmpty List<@Valid ImGroupMessageStatusPo> groupReadStatusList) {
        return Mono.fromCallable(() -> imGroupMessageStatusService.saveBatch(groupReadStatusList)).subscribeOn(Schedulers.boundedElastic());
    }
}
