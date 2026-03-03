package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.StickerPackVo;
import com.xy.lucky.platform.domain.vo.StickerRespVo;
import com.xy.lucky.platform.domain.vo.StickerVo;
import com.xy.lucky.platform.service.StickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * 表情包管理接口
 * - 创建/查询表情包
 * - 上传/下载/查询表情条目
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/sticker")
@RequiredArgsConstructor
@Tag(name = "sticker", description = "表情包管理")
public class StickerController {

    private final StickerService stickerService;

    @Operation(summary = "查询表情包详情", description = "按 packId 查询表情包")
    @GetMapping("/pack/{packId}")
    public Mono<StickerRespVo> getPackId(@Parameter(description = "包编码", required = true) @PathVariable("packId") String packId) {
        return Mono.fromCallable(() -> stickerService.getPackId(packId)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "获取表情包编码", description = "返回表情包的code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/pack/code")
    public Mono<String> getPackCode() {
        log.info("收到获取表情包编码请求");
        return Mono.fromCallable(() -> stickerService.getPackCode(0)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "创建或更新表情包", description = "根据 code 创建或更新表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StickerPackVo.class)))
    })
    @PostMapping("/pack")
    public Mono<StickerPackVo> upsertPack(@Valid @RequestBody StickerPackVo request) {
        log.info("收到表情包创建/更新请求，code={}", request.getCode());
        return Mono.fromCallable(() -> stickerService.upsertPack(request)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "列出所有表情包", description = "返回所有可用表情包列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StickerPackVo.class)))
    })
    @GetMapping("/pack/list")
    public Mono<List<StickerPackVo>> listPacks() {
        return Mono.fromCallable(stickerService::listPacks).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "上传表情", description = "将图片上传到指定表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StickerVo.class)))
    })
    @PostMapping("/upload")
    public Mono<StickerVo> upload(
            @Parameter(description = "表情元数据", required = true) @Valid StickerVo stickerVo
    ) {
        log.info("收到表情上传请求，packId={} name={}", stickerVo.getPackId(), stickerVo.getName());
        return Mono.fromCallable(() -> stickerService.uploadSticker(stickerVo)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "批量上传表情", description = "一次上传多个图片文件到指定表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StickerVo.class)))
    })
    @PostMapping("/pack/{packId}/upload/batch")
    public Mono<List<StickerVo>> uploadBatch(
            @Parameter(description = "表情包ID", required = true) @PathVariable("packId") String packId,
            @Parameter(description = "图片文件列表", required = true) @RequestPart("files") List<FilePart> files,
            @Parameter(description = "统一标签（可选）") @RequestParam(value = "tags", required = false) String tags
    ) {
        log.info("收到批量上传请求，packId={} 文件数={}", packId, files == null ? 0 : files.size());
        return Mono.fromCallable(() -> emojiService.uploadStickerBatch(packId, files, tags)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "列出表情包内的所有表情", description = "按 packId 返回表情列表")
    @GetMapping("/pack/{packId}/items")
    public Mono<List<StickerVo>> listByPack(@NotBlank(message = "packId 不能为空") @PathVariable("packId") String packId) {
        return Mono.fromCallable(() -> emojiService.listStickers(packId)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "下载表情文件", description = "流式传输图片文件")
    @GetMapping("/item/{emojiId}/download")
    public Mono<ResponseEntity<Resource>> download(@NotBlank(message = "emojiId 不能为空") @PathVariable("emojiId") String emojiId) {
        return Mono.fromCallable(() -> emojiService.downloadSticker(emojiId)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "上传表情包封面", description = "上传封面图片并更新表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StickerPackVo.class)))
    })
    @PostMapping("/pack/{packId}/cover")
    public Mono<StickerPackVo> uploadCover(
            @Parameter(description = "表情包ID", required = true) @PathVariable("packId") String packId,
            @Parameter(description = "封面图片", required = true) @RequestPart("file") FilePart file
    ) {
        return Mono.fromCallable(() -> emojiService.uploadCover(packId, file)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "启用/禁用表情包", description = "按 packId 切换启用状态")
    @PostMapping("/pack/{packId}/enable/{enabled}")
    public Mono<StickerPackVo> toggle(
            @Parameter(description = "包编码", required = true) @PathVariable("packId") String packId,
            @Parameter(description = "是否启用", required = true) @PathVariable("enabled") boolean enabled
    ) {
        return Mono.fromCallable(() -> emojiService.togglePack(packId, enabled)).subscribeOn(Schedulers.boundedElastic());
    }

    @Operation(summary = "删除表情", description = "可选同时删除 MinIO 对象")
    @DeleteMapping("/item/{emojiId}")
    public Mono<String> deleteSticker(
            @Parameter(description = "表情ID", required = true) @PathVariable("emojiId") String emojiId,
            @Parameter(description = "是否删除对象", required = false) @RequestParam(value = "removeObject", defaultValue = "true") boolean removeObject
    ) {
        return Mono.fromRunnable(() -> emojiService.deleteSticker(emojiId, removeObject))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn("OK");
    }
}
