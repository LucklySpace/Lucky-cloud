package com.xy.ai.controller;

import com.xy.ai.service.EmbeddingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/file")
public class EmbeddingController {

    @Resource
    private EmbeddingService embeddingService;

    @PostMapping("/add")
    public void add(@RequestParam(name = "file") MultipartFile file) {
        embeddingService.add(file);
    }

    @GetMapping("/search")
    public List<String> search(@RequestParam(name = "text") String text) {
        return embeddingService.search(text);
    }
}