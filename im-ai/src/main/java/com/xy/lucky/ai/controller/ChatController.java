package com.xy.lucky.ai.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * MessageController 提供对单条消息的管理接口：
 * - 发送消息
 * - 获取最近 N 条消息
 * - 删除指定消息
 * - 重新生成 AI 回复
 */
@Slf4j
@RestController
@RequestMapping({"/api/chat", "/api/{version}/ai/chat"})
@Tag(name = "chat", description = "对话接口")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    @Resource
    @Qualifier("chatPostgresMemory")
    private ChatMemory chatPostgresMemory;


    /**
     * 简单单轮问答接口（无上下文）
     *
     * @param text 用户输入
     * @return AI 的响应文本
     */
    @GetMapping("/ask")
    @Operation(summary = "单轮问答")
    public String ask(@RequestParam("text") String text) {
        log.info("[chat] 用户提问：{}", text);
        return chatClient.prompt().user(text).call().content().trim();
    }

    /**
     * 多轮对话接口
     *
     * @param sessionId 会话标识
     * @param text      用户输入
     * @return AI 响应文本
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "多轮对话SSE流")
    public Flux<ServerSentEvent<String>> chat(@RequestParam("sessionId") String sessionId, @RequestParam(value = "text", defaultValue = "Hello!") String text) {
        log.info("[chat] 会话 {} 用户输入：{}", sessionId, text);
        return chatClient.prompt(text)
                .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .stream().content()
                .map(content -> ServerSentEvent.builder(content).event("message").build())

                .concatWithValues(
                        ServerSentEvent.builder("[DONE]")
                                .event("end")
                                .build()
                )
                .onErrorResume(e -> {
                    log.error("SSE 发生错误: {}", e.getMessage(), e);
                    return Flux.just(ServerSentEvent.builder("Error: " + e.getMessage())
                            .event("error")
                            .build());
                });
    }
}

