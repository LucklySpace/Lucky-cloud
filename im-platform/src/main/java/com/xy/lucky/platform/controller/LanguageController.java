package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.LanguagePackVo;
import com.xy.lucky.platform.service.LanguageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * 语言包管理接口
 * - 新增/修改语言包元信息
 * - 上传/下载语言包文件
 */
@Slf4j
@Validated
@RestController
@RequestMapping({"/api/lang", "/api/{version}/lang"})
@RequiredArgsConstructor
@Tag(name = "language", description = "语言包管理")
public class LanguageController {

    private final LanguageService languageService;

    @Operation(summary = "新增或修改语言包", description = "根据 locale 创建或更新语言包元信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LanguagePackVo.class)))
    })
    @PostMapping("/pack")
    public Mono<LanguagePackVo> upsert(@Valid @RequestBody LanguagePackVo request) {
        log.info("收到语言包创建/更新请求，locale={}", request.getLocale());
        return Mono.fromCallable(() -> languageService.upsert(request)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "列出所有语言包", description = "返回所有语言包列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LanguagePackVo.class)))
    })
    @GetMapping("/pack/list")
    public Mono<List<LanguagePackVo>> list() {
        return Mono.fromCallable(languageService::listAll).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "上传语言包文件", description = "上传语言包JSON或压缩包并更新元信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LanguagePackVo.class)))
    })
    @PostMapping("/upload")
    public Mono<LanguagePackVo> upload(
            @Parameter(description = "语言包元信息", required = true) @Valid @RequestPart("meta") LanguagePackVo meta,
            @Parameter(description = "语言包文件", required = true) @RequestPart("file") FilePart file
    ) {
        log.info("收到语言包上传请求，locale={} version={} file={}", meta.getLocale(), meta.getVersion(), file.filename());
        return Mono.fromCallable(() -> languageService.upload(meta, file)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "按 locale 下载语言包", description = "流式传输语言包文件")
    @GetMapping("/pack/{locale}/download")
    public Mono<ResponseEntity<Resource>> download(
            @Parameter(description = "地区/语言标识", required = true) @PathVariable("locale") String locale
    ) {
        return Mono.fromCallable(() -> languageService.download(locale)).subscribeOn(Schedulers.boundedElastic());
    }
}

