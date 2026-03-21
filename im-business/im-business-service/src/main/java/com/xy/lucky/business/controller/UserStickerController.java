package com.xy.lucky.business.controller;

import com.xy.lucky.business.domain.vo.StickerRespVo;
import com.xy.lucky.business.domain.vo.StickerVo;
import com.xy.lucky.business.service.UserStickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping({"/api/sticker", "/api/{version}/sticker"})
@Tag(name = "sticker-user", description = "用户与表情包关联接口")
@Validated
public class UserStickerController {

    @Resource
    private UserStickerService userStickerService;

    @Resource
    @Qualifier("virtualThreadExecutor")
    private Executor virtualThreadExecutor;

    private Scheduler getScheduler() {
        return Schedulers.fromExecutor(virtualThreadExecutor);
    }

    @GetMapping("/list")
    @Operation(summary = "查询用户已关联的表情包编码列表", tags = {"sticker-user"}, description = "通过用户ID查询其已绑定的表情包编码列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<List<String>> list(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return Mono.fromCallable(() -> userStickerService.listPackIds(userId))
                .subscribeOn(getScheduler());
    }

    @Operation(summary = "查询表情详情", description = "按 stickerId 获取表情包详情")
    @GetMapping("/{stickerId}")
    public Mono<StickerVo> getStickerId(@Parameter(description = "表情编码", required = true) @PathVariable("stickerId") String stickerId) {
        return Mono.fromCallable(() -> userStickerService.getStickerId(stickerId)).subscribeOn(getScheduler());
    }

    @Operation(summary = "查询表情包详情", description = "按 packId 查询表情包")
    @GetMapping("/pack/{packId}")
    public Mono<StickerRespVo> getPackId(@Parameter(description = "包编码", required = true) @PathVariable("packId") String packId) {
        return Mono.fromCallable(() -> userStickerService.getPackId(packId)).subscribeOn(getScheduler());
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定用户与表情包", tags = {"sticker-user"}, description = "为指定用户绑定一个表情包编码")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "packId", description = "表情包编码", required = true, in = ParameterIn.QUERY)
    })
    public Mono<Boolean> bind(@RequestParam("userId") @NotBlank @Size(max = 64) String userId,
                              @RequestParam("packId") @NotBlank @Size(max = 64) String packId) {
        return Mono.fromCallable(() -> userStickerService.bindPack(userId, packId))
                .subscribeOn(getScheduler());
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "解绑用户与表情包", tags = {"sticker-user"}, description = "为指定用户解绑一个表情包编码")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "packId", description = "表情包编码", required = true, in = ParameterIn.QUERY)
    })
    public Mono<Boolean> unbind(@RequestParam("userId") @NotBlank @Size(max = 64) String userId,
                                @RequestParam("packId") @NotBlank @Size(max = 64) String packId) {
        return Mono.fromCallable(() -> userStickerService.unbindPack(userId, packId))
                .subscribeOn(getScheduler());
    }
}
