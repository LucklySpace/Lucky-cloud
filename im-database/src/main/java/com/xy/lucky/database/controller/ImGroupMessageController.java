package com.xy.lucky.database.controller;

import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImGroupMessageService;
import com.xy.lucky.database.service.ImGroupMessageStatusService;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImGroupMessageStatusPo;
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
@RequestMapping("/api/{version}/database/group/message")
@Tag(name = "ImGroup", description = "群聊消息数据库接口")
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
    public ImGroupMessagePo getGroupMessageById(@RequestParam("messageId") String messageId) {
        return imGroupMessageService.queryOne(messageId);
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
    public List<ImGroupMessagePo> listGroupMessages(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imGroupMessageService.queryList(userId, sequence);
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
    public Boolean createGroupMessage(@RequestBody ImGroupMessagePo messagePo) {
        return imGroupMessageService.creat(messagePo);
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
    public Boolean createGroupMessageStatusBatch(@RequestBody List<ImGroupMessageStatusPo> messagePoList) {
        return imGroupMessageService.creatBatch(messagePoList);
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
    public Boolean updateGroupMessage(@RequestBody ImGroupMessagePo messagePo) {
        return imGroupMessageService.modify(messagePo);
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
    public Boolean deleteGroupMessageById(@RequestParam("messageId") String messageId) {
        return imGroupMessageService.removeOne(messageId);
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
    public ImGroupMessagePo getLastGroupMessage(@RequestParam("groupId") String groupId, @RequestParam("userId") String userId) {
        return imGroupMessageService.queryLast(groupId, userId);
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
    public Integer getGroupMessageReadStatus(@RequestParam("groupId") String groupId, @RequestParam("toId") String toId, @RequestParam("code") Integer code) {
        return imGroupMessageService.queryReadStatus(groupId, toId, code);
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
    public Boolean insertGroupMessageStatusBatch(@RequestBody List<ImGroupMessageStatusPo> groupReadStatusList) {
        return imGroupMessageStatusService.saveBatch(groupReadStatusList);
    }
}
