package com.xy.lucky.server.controller;

import com.xy.lucky.server.service.UserEmojiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/{version}/emoji/userPack")
@Tag(name = "emoji-user", description = "用户与表情包关联接口")
@Validated
public class UserEmojiController {

    @Resource
    private UserEmojiService userEmojiService;

    @GetMapping("/list")
    @Operation(summary = "查询用户已关联的表情包编码列表", tags = {"emoji-user"}, description = "通过用户ID查询其已绑定的表情包编码列表")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY)
    })
    public Mono<List<String>> list(@RequestParam("userId") @NotBlank @Size(max = 64) String userId) {
        return userEmojiService.listPackIds(userId);
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定用户与表情包", tags = {"emoji-user"}, description = "为指定用户绑定一个表情包编码")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "packId", description = "表情包编码", required = true, in = ParameterIn.QUERY)
    })
    public Mono<Boolean> bind(@RequestParam("userId") @NotBlank @Size(max = 64) String userId,
                              @RequestParam("packId") @NotBlank @Size(max = 64) String packId) {
        return userEmojiService.bindPack(userId, packId);
    }

    @DeleteMapping("/unbind")
    @Operation(summary = "解绑用户与表情包", tags = {"emoji-user"}, description = "为指定用户解绑一个表情包编码")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "packId", description = "表情包编码", required = true, in = ParameterIn.QUERY)
    })
    public Mono<Boolean> unbind(@RequestParam("userId") @NotBlank @Size(max = 64) String userId,
                                @RequestParam("packId") @NotBlank @Size(max = 64) String packId) {
        return userEmojiService.unbindPack(userId, packId);
    }
}

