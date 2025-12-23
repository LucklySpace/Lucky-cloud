package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.CreateShortLinkVo;
import com.xy.lucky.platform.domain.vo.ShortLinkVo;
import com.xy.lucky.platform.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 短链控制器
 * 提供创建、解析、查询与禁用短链接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/short")
@RequiredArgsConstructor
@Tag(name = "short", description = "短链服务接口")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    /**
     * 创建短链
     */
    @Operation(summary = "创建短链", description = "输入原始URL，返回短码与短链信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShortLinkVo.class)))
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true)
    })
    @PostMapping("/create")
    public ShortLinkVo createShortLink(@Valid @RequestBody CreateShortLinkVo request) {
        log.info("收到短链创建请求，url={}", request.getOriginalUrl());
        return shortLinkService.createShortLink(request);
    }

    /**
     * 查询短链信息
     */
    @Operation(summary = "查询短链信息", description = "返回短链的原始URL、访问次数、过期时间与启用状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShortLinkVo.class))),
            @ApiResponse(responseCode = "404", description = "短码不存在")
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true),
            @Parameter(in = ParameterIn.PATH, name = "code", description = "短码", required = true)
    })
    @GetMapping("/info/{code}")
    public ShortLinkVo info(@NotBlank(message = "短码不能为空") @PathVariable("code") String code) {
        log.info("收到短链信息查询请求，code={}", code);
        return shortLinkService.info(code);
    }

    /**
     * 禁用短链
     */
    @Operation(summary = "禁用短链", description = "禁用指定短码的短链")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "禁用成功")
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "version", description = "API 路径版本（占位）", required = true),
            @Parameter(in = ParameterIn.PATH, name = "code", description = "短码", required = true)
    })
    @PostMapping("/disable/{code}")
    public String disable(@NotBlank(message = "短码不能为空") @PathVariable("code") String code) {
        log.info("收到短链禁用请求，code={}", code);
        shortLinkService.disable(code);
        return "OK";
    }
}
