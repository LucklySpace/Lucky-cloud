package com.xy.lucky.ai.controller;

import com.xy.lucky.ai.domain.vo.ChatPromptVo;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/prompts", "/api/{version}/ai/prompts"})
@Tag(name = "prompt", description = "增删改查 Prompt 配置")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @Operation(summary = "列出所有 Prompt")
    @ApiResponse(responseCode = "200", description = "返回 Prompt 列表")
    @GetMapping
    @Cacheable(cacheNames = "prompts", key = "'all'")
    public List<ChatPromptVo> list() {
        log.info("[prompt] 列出所有 Prompt");
        return promptService.list();
    }

    @Operation(summary = "添加新的 Prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prompt 添加成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效或不包含 {topic} 占位符")
    })
    @PostMapping
    @CacheEvict(cacheNames = "prompts", key = "'all'")
    public boolean add(@RequestBody ChatPromptVo chatPrompt) {
        log.debug("[prompt] 添加新的 Prompt: {}", chatPrompt);
        return promptService.add(chatPrompt);
    }

    @Operation(summary = "更新已有的 Prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prompt 更新成功"),
            @ApiResponse(responseCode = "400", description = "请求参数无效或不包含 {topic} 占位符"),
            @ApiResponse(responseCode = "404", description = "Prompt ID 不存在")
    })
    @PutMapping("/{id}")
    @CacheEvict(cacheNames = "prompts", key = "'all'")
    public boolean update(
            @Parameter(description = "Prompt ID", required = true) @PathVariable("id") String id,
            @RequestBody ChatPromptVo chatPrompt
    ) {
        log.debug("[prompt] 更新 Prompt: {}", chatPrompt);
        chatPrompt.setId(id);
        return promptService.update(chatPrompt);
    }

    @Operation(summary = "删除指定 Prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prompt 删除成功"),
            @ApiResponse(responseCode = "404", description = "Prompt ID 不存在")
    })
    @DeleteMapping("/{id}")
    @CacheEvict(cacheNames = "prompts", key = "'all'")
    public boolean delete(@Parameter(description = "Prompt ID", required = true) @PathVariable("id") String id) {
        log.debug("[prompt] 删除 Prompt: {}", id);
        return promptService.delete(id);
    }
}
