package com.xy.lucky.ai.controller;

import com.xy.lucky.ai.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@RestController
@RequestMapping({"/api/{version}/ai/embedding", "/api/ai/embedding", "/api/file"})
@Tag(name = "embedding", description = "知识库向量管理")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    @PostMapping(value = "/add", consumes = {"multipart/form-data"})
    @Operation(summary = "添加文档到向量库")
    public void add(@RequestParam(name = "file") MultipartFile file) {
        embeddingService.add(file);
    }

    @GetMapping("/search")
    @Operation(summary = "向量语义搜索")
    public List<String> search(@RequestParam(name = "text") String text) {
        return embeddingService.search(text);
    }
}
