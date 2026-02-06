package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.EmailVo;
import com.xy.lucky.platform.domain.vo.SmsVo;
import com.xy.lucky.platform.service.NotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/notify")
@RequiredArgsConstructor
@Tag(name = "notify", description = "通知服务接口：邮件与短信")
public class NotifyController {

    private final NotifyService notifyService;

    @Operation(summary = "发送邮件")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "邮件发送成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class)))
    })
    @PostMapping("/email/send")
    public Mono<Boolean> sendEmail(@Valid @RequestBody EmailVo vo) {
        return Mono.fromCallable(() -> notifyService.sendEmail(vo))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "发送短信")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "短信发送成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class)))
    })
    @PostMapping("/sms/send")
    public Mono<Boolean> sendSms(@Valid @RequestBody SmsVo vo) {
        return Mono.fromCallable(() -> notifyService.sendSms(vo))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
