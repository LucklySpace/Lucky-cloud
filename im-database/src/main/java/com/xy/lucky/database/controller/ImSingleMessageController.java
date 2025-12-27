package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImSingleMessageService;
import com.xy.lucky.domain.po.ImSingleMessagePo;
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
@RequestMapping("/api/{version}/database/single/message")
@Tag(name = "ImPrivate", description = "私聊消息数据库接口")
public class ImSingleMessageController {

    @Resource
    private ImSingleMessageService imSingleMessageService;


    /**
     * 根据id查询私聊消息
     *
     * @param messageId 私聊消息id
     * @return 私聊消息
     */
    @GetMapping("/selectOne")
    @Operation(summary = "根据ID获取私聊消息", description = "返回指定消息ID的单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImSingleMessagePo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImSingleMessagePo getSingleMessageById(@RequestParam("messageId") String messageId) {
        return imSingleMessageService.queryOne(messageId);
    }

    /**
     * 获取用户私聊消息
     *
     * @param userId   用户id
     * @param sequence 时间序列
     * @return 用户私聊消息
     */
    @GetMapping("/selectList")
    @Operation(summary = "获取用户单聊消息列表", description = "按用户ID与序列获取单聊消息列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImSingleMessagePo.class)))
    })
    public List<ImSingleMessagePo> listSingleMessages(@RequestParam("userId") String userId, @RequestParam("sequence") Long sequence) {
        return imSingleMessageService.queryList(userId, sequence);
    }

    /**
     * 获取最后一条消息
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @return 私聊消息
     */
    @GetMapping("/last")
    @Operation(summary = "获取最后一条单聊消息", description = "按双方ID查询最新单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImSingleMessagePo.class)))
    })
    public ImSingleMessagePo getLastSingleMessage(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId) {
        return imSingleMessageService.queryLast(fromId, toId);
    }

    /**
     * 查询阅读状态
     *
     * @param fromId 发送者id
     * @param toId   接受者id
     * @param code   阅读状态码
     * @return 未/已读消息数
     */
    @GetMapping("/selectReadStatus")
    @Operation(summary = "查询单聊消息阅读状态", description = "按双方ID与状态码统计消息阅读状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Integer getSingleMessageReadStatus(@RequestParam("fromId") String fromId, @RequestParam("toId") String toId, @RequestParam("code") Integer code) {
        return imSingleMessageService.queryReadStatus(fromId, toId, code);
    }

    /**
     * 插入私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/insert")
    @Operation(summary = "创建单聊消息", description = "新增单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createSingleMessage(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.creat(messagePo);
    }


    /**
     * 删除私聊消息
     *
     * @param messageId 私聊消息id
     */
    @DeleteMapping("/deleteById")
    @Operation(summary = "删除单聊消息", description = "根据ID删除单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteSingleMessageById(@RequestParam("messageId") String messageId) {
        return imSingleMessageService.removeOne(messageId);
    }
    
    /**
     * 批量插入私聊消息
     *
     * @param messagePoList 私聊消息列表
     */
    @PostMapping("/batchInsert")
    @Operation(summary = "批量创建单聊消息", description = "批量新增单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createSingleMessagesBatch(@RequestBody List<ImSingleMessagePo> messagePoList) {
        return imSingleMessageService.creatBatch(messagePoList);
    }

    /**
     * 插入或更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PostMapping("/saveOrUpdate")
    @Operation(summary = "新增或更新单聊消息", description = "存在则更新，不存在则新增")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功")
    })
    public Boolean upsertSingleMessage(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.saveOrUpdate(messagePo);
    }
    
    /**
     * 更新私聊消息
     *
     * @param messagePo 私聊消息
     */
    @PutMapping("/update")
    @Operation(summary = "更新单聊消息", description = "根据ID更新单聊消息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateSingleMessage(@RequestBody ImSingleMessagePo messagePo) {
        return imSingleMessageService.modify(messagePo);
    }

}
