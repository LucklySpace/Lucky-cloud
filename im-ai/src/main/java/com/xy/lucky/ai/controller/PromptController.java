package com.xy.lucky.ai.controller;

import com.xy.lucky.ai.domain.ChatPrompt;
import com.xy.lucky.ai.service.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/prompts")
@Tag(name = "Prompt 管理", description = "增删改查 Prompt 配置")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @Operation(summary = "列出所有 Prompt")
    @ApiResponse(responseCode = "200", description = "返回 Prompt 列表")
    @GetMapping
    @Cacheable(cacheNames = "prompts", key = "'all'")
    public ResponseEntity<List<ChatPrompt>> list() {
        log.info("从数据库加载所有 Prompt");
        List<ChatPrompt> prompts = promptService.list();
        return ResponseEntity.ok(prompts);
    }

    @Operation(summary = "添加新的 Prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prompt 添加成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效或不包含 {topic} 占位符")
    })
    @PostMapping
    @CacheEvict(cacheNames = "prompts", key = "'all'")
    public ResponseEntity<Boolean> add(
            @RequestBody ChatPrompt chatPrompt
    ) {
        boolean success = promptService.add(chatPrompt);
        return success
                ? ResponseEntity.status(HttpStatus.CREATED).body(true)
                : ResponseEntity.badRequest().body(false);
    }

    @Operation(summary = "更新已有的 Prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prompt 更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效或不包含 {topic} 占位符"),
            @ApiResponse(responseCode = "404", description = "Prompt ID 不存在")
    })
    @PutMapping("/{id}")
    @CacheEvict(cacheNames = "prompts", key = "'all'")
    public ResponseEntity<Boolean> update(
            @Parameter(description = "Prompt ID", required = true) @PathVariable("id") String id,
            @RequestBody ChatPrompt chatPrompt
    ) {
        chatPrompt.setId(id);
        boolean success = promptService.update(chatPrompt);
        if (!success) {
            // 区分参数错误（占位符缺失）与不存在，可根据业务再细化
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
        }
        return ResponseEntity.ok(true);
    }

    @Operation(summary = "删除指定 Prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prompt 删除成功"),
            @ApiResponse(responseCode = "404", description = "Prompt ID 不存在")
    })
    @DeleteMapping("/{id}")
    @CacheEvict(cacheNames = "prompts", key = "'all'")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Prompt ID", required = true) @PathVariable("id") String id
    ) {
        boolean success = promptService.delete(id);
        return success
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
