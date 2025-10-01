package com.xy.alysis.controller;


import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.seg.common.Term;
import com.xy.alysis.service.HanLPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/alysis")
@Tag(name = "alysis", description = "语义分析")
public class HanLPController {

    // 注入的 HanLP 服务
    @Resource
    private HanLPService hanLPService;


    @Operation(summary = "转换拼音首字母", tags = {"alysis"}, description = "请使用此接口转换并打印拼音首字母")
    @Parameters({
            @Parameter(name = "content", description = "待处理的内容", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/convertAndPinyinFirstLetter")
    public List<String> convertAndPinyinFirstLetter(@RequestParam String content) {
        // 调用服务层的方法提取关键词
        return hanLPService.convertAndPinyinFirstLetter(content);
    }

    @Operation(summary = "相识度分析", tags = {"alysis"}, description = "请使用此接口进行相识度分析")
    @Parameters({
            @Parameter(name = "content1", description = "待处理的内容", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "content2", description = "待处理的内容", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/similarity")
    public double similarity(@RequestParam String content1, @RequestParam String content2) {
        return hanLPService.similarity(content1, content2);
    }


    @Operation(summary = "提取关键词", tags = {"alysis"}, description = "请使用此接口提取关键词")
    @Parameters({
            @Parameter(name = "content", description = "待处理的内容", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "关键词数量", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/extractKeyword")
    public List<String> extractKeyword(@RequestParam String content, @RequestParam int size) {
        return hanLPService.extractKeyword(content, size);
    }

    @Operation(summary = "提取摘要", tags = {"alysis"}, description = "请使用此接口提取摘要")
    @Parameters({
            @Parameter(name = "document", description = "待处理的文档", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "摘要大小", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/extractSummary")
    public List<String> extractSummary(@RequestParam String document, @RequestParam int size) {
        return hanLPService.extractSummary(document, size);
    }

    @Operation(summary = "提取短语", tags = {"alysis"}, description = "请使用此接口提取短语")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "短语数量", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/extractPhrase")
    public List<String> extractPhrase(@RequestParam String text, @RequestParam int size) {
        return hanLPService.extractPhrase(text, size);
    }

    @Operation(summary = "标准分词", tags = {"alysis"}, description = "请使用此接口进行标准分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/standardSegment")
    public List<Term> standardSegment(@RequestParam String text) {
        return hanLPService.standardSegment(text);
    }

    @Operation(summary = "自然语言处理分词", tags = {"alysis"}, description = "请使用此接口进行 NLP 分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/nlpSegment")
    public List<Term> nlpSegment(@RequestParam String text) {
        return hanLPService.nlpSegment(text);
    }

    @Operation(summary = "索引分词", tags = {"alysis"}, description = "请使用此接口进行索引分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/indexSegment")
    public List<Term> indexSegment(@RequestParam String text) {
        return hanLPService.indexSegment(text);
    }

    @Operation(summary = "N 短分词", tags = {"alysis"}, description = "请使用此接口进行 N 短分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/nShortSegment")
    public List<Term> nShortSegment(@RequestParam String text) {
        return hanLPService.nShortSegment(text);
    }

    @Operation(summary = "最短分词", tags = {"alysis"}, description = "请使用此接口进行最短分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/shortestSegment")
    public List<Term> shortestSegment(@RequestParam String text) {
        return hanLPService.shortestSegment(text);
    }

    @Operation(summary = "CRF 分词", tags = {"alysis"}, description = "请使用此接口进行 CRF 分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/crfSegment")
    public String crfSegment(@RequestParam String text) {
        return hanLPService.crfSegment(text);
    }

    @Operation(summary = "快速分词", tags = {"alysis"}, description = "请使用此接口进行快速分词")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/speedSegment")
    public List<Term> speedSegment(@RequestParam String text) {
        return hanLPService.speedSegment(text);
    }

    @Operation(summary = "NLP 分析", tags = {"alysis"}, description = "请使用此接口进行 NLP 分析")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/nlpAnalyze")
    public String nlpAnalyze(@RequestParam String text) {
        return hanLPService.nlpAnalyze(text);
    }

    @Operation(summary = "转换并打印拼音", tags = {"alysis"}, description = "请使用此接口转换文本为拼音并打印")
    @Parameters({
            @Parameter(name = "text", description = "待处理的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/convertAndPrintPinyin")
    public void convertAndPrintPinyin(@RequestParam String text) {
        hanLPService.convertAndPrintPinyin(text);
    }

    @Operation(summary = "转换为繁体字", tags = {"alysis"}, description = "请使用此接口将文本转换为繁体字")
    @Parameters({
            @Parameter(name = "text", description = "待转换的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/convertToTraditional")
    public String convertToTraditional(@RequestParam String text) {
        return hanLPService.convertToTraditional(text);
    }

    @Operation(summary = "转换为简体字", tags = {"alysis"}, description = "请使用此接口将文本转换为简体字")
    @Parameters({
            @Parameter(name = "text", description = "待转换的文本", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/convertToSimplified")
    public String convertToSimplified(@RequestParam String text) {
        return hanLPService.convertToSimplified(text);
    }

    @Operation(summary = "解析依存关系", tags = {"alysis"}, description = "请使用此接口解析句子的依存关系")
    @Parameters({
            @Parameter(name = "sentence", description = "待解析的句子", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/parseDependency")
    public CoNLLSentence parseDependency(@RequestParam String sentence) {
        return hanLPService.parseDependency(sentence);
    }

    @Operation(summary = "格式化依存关系", tags = {"alysis"}, description = "请使用此接口格式化依存关系")
    @Parameters({
            @Parameter(name = "sentence", description = "待格式化的依存关系", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/formatDependency")
    public String formatDependency(@RequestBody CoNLLSentence sentence) {
        return hanLPService.formatDependency(sentence);
    }

    @Operation(summary = "反向格式化依存关系", tags = {"alysis"}, description = "请使用此接口反向格式化依存关系")
    @Parameters({
            @Parameter(name = "sentence", description = "待反向格式化的依存关系", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/formatDependencyReverse")
    public String formatDependencyReverse(@RequestBody CoNLLSentence sentence) {
        return hanLPService.formatDependencyReverse(sentence);
    }

    @Operation(summary = "向上遍历依存关系", tags = {"alysis"}, description = "请使用此接口向上遍历依存关系")
    @Parameters({
            @Parameter(name = "sentence", description = "待遍历的依存关系", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "index", description = "索引位置", required = true, in = ParameterIn.QUERY)
    })
    @PostMapping("/traverseUp")
    public String traverseUp(@RequestBody CoNLLSentence sentence, @RequestParam int index) {
        return hanLPService.traverseUp(sentence, index);
    }
}