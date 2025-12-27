package com.xy.lucky.database.controller;


import com.xy.lucky.database.security.SecurityInner;
import com.xy.lucky.database.service.ImChatService;
import com.xy.lucky.domain.po.ImChatPo;
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
@RestController
@RequestMapping("/api/{version}/database/chat")
@Tag(name = "ImChat", description = "用户会话数据库接口")
public class ImChatController {

    @Resource
    private ImChatService imChatService;

    @GetMapping("/selectList")
    @Operation(summary = "查询用户会话列表", description = "根据用户ID与序列查询会话列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImChatPo.class)))
    })
    public List<ImChatPo> listChats(@RequestParam("ownerId") String ownerId, @RequestParam("sequence") Long sequence) {
        return imChatService.queryList(ownerId, sequence);
    }

    @GetMapping("/selectOne")
    @Operation(summary = "获取单个会话信息", description = "根据ownerId、toId与可选的chatType查询单个会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImChatPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public ImChatPo getChat(@RequestParam("ownerId") String ownerId, @RequestParam("toId") String toId, @RequestParam(value = "chatType", required = false) Integer chatType) {
        return imChatService.queryOne(ownerId, toId, chatType);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建会话", description = "新增会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Boolean createChat(@RequestBody ImChatPo chatPo) {
        return imChatService.creat(chatPo);
    }

    @SecurityInner
    @PutMapping("/update")
    @Operation(summary = "更新会话", description = "根据ID更新会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Boolean updateChat(@RequestBody ImChatPo chatPo) {
        return imChatService.modify(chatPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除会话", description = "根据ID删除会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Boolean deleteChatById(@RequestParam("id") String id) {
        return imChatService.removeOne(id);
    }

}
