package com.xy.lucky.platform.controller;

import com.xy.lucky.platform.domain.vo.EmojiPackVo;
import com.xy.lucky.platform.domain.vo.EmojiVo;
import com.xy.lucky.platform.service.EmojiService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 表情包管理接口
 * - 创建/查询表情包
 * - 上传/下载/查询表情条目
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/{version}/emoji")
@RequiredArgsConstructor
@Tag(name = "emoji", description = "表情包管理")
public class EmojiController {

    private final EmojiService emojiService;


    @Operation(summary = "获取表情包编码", description = "返回表情包的code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/pack/code")
    public String getPackCode() {
        log.info("收到获取表情包编码请求");
        return emojiService.getPackCode();
    }

    @Operation(summary = "创建或更新表情包", description = "根据 code 创建或更新表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmojiPackVo.class)))
    })
    @PostMapping("/pack")
    public EmojiPackVo upsertPack(@Valid @RequestBody EmojiPackVo request) {
        log.info("收到表情包创建/更新请求，code={}", request.getCode());
        return emojiService.upsertPack(request);
    }

    @Operation(summary = "列出所有表情包", description = "返回所有可用表情包列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmojiPackVo.class)))
    })
    @GetMapping("/pack/list")
    public List<EmojiPackVo> listPacks() {
        return emojiService.listPacks();
    }

    @Operation(summary = "上传表情", description = "将图片上传到指定表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmojiVo.class)))
    })
    @PostMapping("/upload")
    public EmojiVo upload(
            @Parameter(description = "表情元数据", required = true) @Valid @RequestPart("emojiVo") EmojiVo emojiVo,
            @Parameter(description = "图片文件", required = true) @RequestPart("file") MultipartFile file
    ) {
        log.info("收到表情上传请求，packId={} name={}", emojiVo.getPackId(), emojiVo.getName());
        return emojiService.uploadEmoji(emojiVo, file);
    }

    @Operation(summary = "批量上传表情", description = "一次上传多个图片文件到指定表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmojiVo.class)))
    })
    @PostMapping("/pack/{packId}/upload/batch")
    public List<EmojiVo> uploadBatch(
            @Parameter(description = "表情包ID", required = true) @PathVariable("packId") String packId,
            @Parameter(description = "图片文件列表", required = true) @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "统一标签（可选）") @RequestParam(value = "tags", required = false) String tags
    ) {
        log.info("收到批量上传请求，packId={} 文件数={}", packId, files == null ? 0 : files.size());
        return emojiService.uploadEmojiBatch(packId, files, tags);
    }

    @Operation(summary = "列出表情包内的所有表情", description = "按 packId 返回表情列表")
    @GetMapping("/pack/{packId}/items")
    public List<EmojiVo> listByPack(@NotBlank(message = "packId 不能为空") @PathVariable("packId") String packId) {
        return emojiService.listEmojis(packId);
    }

    @Operation(summary = "下载表情文件", description = "流式传输图片文件")
    @GetMapping("/item/{emojiId}/download")
    public ResponseEntity<Resource> download(@NotBlank(message = "emojiId 不能为空") @PathVariable("emojiId") String emojiId) {
        return emojiService.downloadEmoji(emojiId);
    }

    @Operation(summary = "上传表情包封面", description = "上传封面图片并更新表情包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "上传成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmojiPackVo.class)))
    })
    @PostMapping("/pack/{packId}/cover")
    public EmojiPackVo uploadCover(
            @Parameter(description = "表情包ID", required = true) @PathVariable("packId") String packId,
            @Parameter(description = "封面图片", required = true) @RequestPart("file") MultipartFile file
    ) {
        return emojiService.uploadCover(packId, file);
    }

    @Operation(summary = "启用/禁用表情包", description = "按 code 切换启用状态")
    @PostMapping("/pack/{code}/enable/{enabled}")
    public EmojiPackVo toggle(
            @Parameter(description = "包编码", required = true) @PathVariable("code") String code,
            @Parameter(description = "是否启用", required = true) @PathVariable("enabled") boolean enabled
    ) {
        return emojiService.togglePack(code, enabled);
    }

    @Operation(summary = "查询表情包详情", description = "按 code 查询表情包")
    @GetMapping("/pack/{code}")
    public EmojiPackVo getPack(@Parameter(description = "包编码", required = true) @PathVariable("code") String code) {
        return emojiService.getPack(code);
    }

    @Operation(summary = "删除表情", description = "可选同时删除 MinIO 对象")
    @DeleteMapping("/item/{emojiId}")
    public String deleteEmoji(
            @Parameter(description = "表情ID", required = true) @PathVariable("emojiId") String emojiId,
            @Parameter(description = "是否删除对象", required = false) @RequestParam(value = "removeObject", defaultValue = "true") boolean removeObject
    ) {
        emojiService.deleteEmoji(emojiId, removeObject);
        return "OK";
    }
}
