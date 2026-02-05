package com.xy.lucky.database.web.controller;

import com.xy.lucky.database.web.security.SecurityInner;
import com.xy.lucky.database.web.service.ImUserEmojiPackService;
import com.xy.lucky.domain.po.ImUserEmojiPackPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/{version}/database/emoji/user-pack")
@Tag(name = "ImUserEmojiPack", description = "用户-表情包关联数据库接口")
@Validated
public class ImUserEmojiPackController {

    @Resource
    private ImUserEmojiPackService service;

    @GetMapping("/list")
    @Operation(summary = "查询用户已关联的表情包编码列表", description = "返回指定用户ID的表情包编码列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    public Mono<List<String>> listPackCodes(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> service.listPackIds(userId)).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/listDetails")
    @Operation(summary = "查询用户已关联的表情包详情", description = "返回指定用户ID的关联记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImUserEmojiPackPo.class)))
    })
    public Mono<List<ImUserEmojiPackPo>> listDetails(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> service.listByUserId(userId)).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定用户-表情包", description = "为用户绑定一个表情包编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<Boolean> bind(@RequestParam("userId") @NotBlank @Size(max = 64) String userId,
                              @RequestParam("packId") @NotBlank @Size(max = 64) String packId) {
        return Mono.fromCallable(() -> service.bindPack(userId, packId)).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "解绑用户-表情包", description = "为用户解绑一个表情包编码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功")
    })
    public Mono<Boolean> unbind(@RequestParam("userId") @NotBlank @Size(max = 64) String userId,
                                @RequestParam("packId") @NotBlank @Size(max = 64) String packId) {
        return Mono.fromCallable(() -> service.unbindPack(userId, packId)).subscribeOn(Schedulers.boundedElastic());
    }
}

