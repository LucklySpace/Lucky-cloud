package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.OcrRequestVo;
import com.xy.lucky.platform.domain.vo.OcrResponseVo;
import com.xy.lucky.platform.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Validated
@RestController
@RequestMapping({"/api/ocr", "/api/{version}/ocr"})
@RequiredArgsConstructor
@Tag(name = "ocr", description = "PaddleOCR 文字识别接口")
public class OcrController {

    private final OcrService ocrService;

    @Operation(summary = "基于 Base64 的通用文字识别", description = "兼容 PaddleOCR 镜像的 /predict/ocr_system 接口")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "识别成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OcrResponseVo.class)))
    })
    @PostMapping("/system")
    public Mono<OcrResponseVo> recognize(@Valid @RequestBody OcrRequestVo req) {
        return Mono.fromCallable(() -> ocrService.recognizeBase64(req.getImages()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "上传图片进行识别", description = "上传单张图片，服务端转换为 Base64 并调用 PaddleOCR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "识别成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OcrResponseVo.class)))
    })
    @PostMapping("/upload")
    public Mono<OcrResponseVo> recognizeByUpload(@RequestPart("file") FilePart file) {
        return file.content()
                .reduce(new byte[0], (acc, dataBuffer) -> {
                    int readable = dataBuffer.readableByteCount();
                    byte[] next = new byte[acc.length + readable];
                    System.arraycopy(acc, 0, next, 0, acc.length);
                    dataBuffer.read(next, acc.length, readable);
                    return next;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(ocrService::recognizeBytes);
    }

    @Operation(summary = "识别网络图片 URL 列表", description = "服务端下载图片并转换为 Base64 调用 PaddleOCR")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "识别成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OcrResponseVo.class)))
    })
    @PostMapping("/url")
    public Mono<OcrResponseVo> recognizeByUrls(@Valid @RequestBody com.xy.lucky.platform.domain.vo.OcrUrlRequestVo req) {
        return Mono.fromCallable(() -> ocrService.recognizeUrls(req.getUrls()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "识别单个网络图片 URL", description = "GET 方式识别单个 URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "识别成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = OcrResponseVo.class)))
    })
    @GetMapping("/url")
    public Mono<OcrResponseVo> recognizeByUrl(@RequestParam("url") String url) {
        return Mono.fromCallable(() -> ocrService.recognizeUrl(url))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
