package com.xy.lucky.database.webflux.controller;

import com.xy.lucky.database.webflux.service.ImUserEmojiPackReactiveService;
import com.xy.lucky.domain.po.ImUserEmojiPackPo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/{version}/database/emoji/user-pack")
@Tag(name = "ImUserEmojiPack", description = "用户-表情包关联数据库接口")
@RequiredArgsConstructor
public class ImUserEmojiPackReactiveController {

    private final ImUserEmojiPackReactiveService service;

    @GetMapping("/list")
    @Operation(summary = "查询用户已关联的表情包编码列表")
    public Flux<String> listPackCodes(@RequestParam("userId") String userId) {
        return service.listPackIds(userId);
    }

    @GetMapping("/listDetails")
    @Operation(summary = "查询用户已关联的表情包详情")
    public Flux<ImUserEmojiPackPo> listDetails(@RequestParam("userId") String userId) {
        return service.listByUserId(userId);
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定用户-表情包")
    public Mono<Boolean> bind(@RequestParam("userId") String userId,
                              @RequestParam("packId") String packId) {
        return service.bindPack(userId, packId);
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "解绑用户-表情包")
    public Mono<Boolean> unbind(@RequestParam("userId") String userId,
                                @RequestParam("packId") String packId) {
        return service.unbindPack(userId, packId);
    }
}
