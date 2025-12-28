package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 短链控制器
 * 提供短链重定向接口
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Tag(name = "short", description = "短链重定向服务接口")
public class RedirectController {

    private final ShortLinkService shortLinkService;

    /**
     * 解析短码并重定向
     */
    @Operation(summary = "解析短码并重定向", description = "使用此接口进行url重定向")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "重定向跳转到原始URL"),
            @ApiResponse(responseCode = "404", description = "短码不存在"),
            @ApiResponse(responseCode = "410", description = "短链已失效或被禁用")
    })
    @Parameters({
            @Parameter(in = ParameterIn.PATH, name = "code", description = "短码", required = true)
    })
    @GetMapping("/{code}")
    public Mono<ResponseEntity<Void>> redirect(@NotBlank(message = "短码不能为空") @PathVariable("code") String code) {
        log.info("收到短码解析请求，code={}", code);
        return Mono.fromCallable(() -> shortLinkService.redirect(code))
                .subscribeOn(Schedulers.boundedElastic());
    }

}
