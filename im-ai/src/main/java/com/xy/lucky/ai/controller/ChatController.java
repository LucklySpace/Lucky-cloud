package com.xy.lucky.ai.controller;


import com.xy.lucky.ai.tools.time.DateTimeTool;
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
        log.info("[ask] 用户提问：{}", text);
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

        return chatClient.prompt(text).tools(new DateTimeTool())
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
//        log.info("[chat] 会话 {} 用户输入：{}", sessionId, text);
//
//        return chatClient.prompt(text)
//                .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
//                .stream().content().map(content -> ServerSentEvent.builder(content).event("message").build())
//                //问题回答结速标识,以便前端消息展示处理
//                .concatWithValues(ServerSentEvent.builder("").build())
//                .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Error: " + e.getMessage()).event("error").build()));
    }


//    /**
//     * 获取历史记录（最多 20 条）
//     *
//     * @param conversationId 会话 ID
//     * @return 历史消息列表
//     */
//    @GetMapping("/history")
//    public List<Message> history(@RequestParam String conversationId) {
//        List<Message> history = chatMemory.get(conversationId, 20);
//        log.info("[history] 获取会话 {} 的历史，共 {} 条记录", conversationId, history.size());
//        return history;
//    }
//
//    /**
//     * 清空历史记录
//     *
//     * @param conversationId 会话 ID
//     */
//    @DeleteMapping("/clear")
//    public void clear(@RequestParam String conversationId) {
//        chatMemory.clear(conversationId);
//        log.info("[clear] 会话 {} 已清除历史记录", conversationId);
//    }


}

