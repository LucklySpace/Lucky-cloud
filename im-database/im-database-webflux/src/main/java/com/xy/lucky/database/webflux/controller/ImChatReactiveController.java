package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImChatReactiveService;
import com.xy.lucky.domain.po.ImChatPo;
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
@RequestMapping("/api/{version}/database/chat")
@Tag(name = "ImChat", description = "用户会话数据库接口(WebFlux-R2DBC)")
@Validated
public class ImChatReactiveController {

    @Resource
    private ImChatReactiveService imChatService;

    @GetMapping("/selectList")
    @Operation(summary = "查询用户会话列表", description = "根据用户ID与序列查询会话列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImChatPo.class)))
    })
    public Mono<List<ImChatPo>> listChats(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                          @RequestParam("sequence") @NotNull @PositiveOrZero Long sequence) {
        return imChatService.queryList(ownerId, sequence).collectList();
    }

    @GetMapping("/selectOne")
    @Operation(summary = "获取单个会话信息", description = "根据ownerId、toId与可选的chatType查询单个会话")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImChatPo.class))),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<ImChatPo> getChat(@RequestParam("ownerId") @NotBlank @Size(max = 64) String ownerId,
                                  @RequestParam("toId") @NotBlank @Size(max = 64) String toId,
                                  @RequestParam(value = "chatType", required = false) @Min(0) Integer chatType) {
        return imChatService.queryOne(ownerId, toId, chatType);
    }

    @PostMapping("/insert")
    @Operation(summary = "创建会话", description = "新增会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功")
    })
    public Mono<Boolean> createChat(@RequestBody @Valid ImChatPo chatPo) {
        return imChatService.create(chatPo);
    }

    @PutMapping("/update")
    @Operation(summary = "更新会话", description = "根据ID更新会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功")
    })
    public Mono<Boolean> updateChat(@RequestBody @Valid ImChatPo chatPo) {
        return imChatService.modify(chatPo);
    }

    @DeleteMapping("/deleteById")
    @Operation(summary = "删除会话", description = "根据ID删除会话信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "未找到")
    })
    public Mono<Boolean> deleteChatById(@RequestParam("id") @NotBlank @Size(max = 64) String id) {
        return imChatService.removeOne(id);
    }
}
