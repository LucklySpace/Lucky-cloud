//
//
//package com.xy.ai.controller;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerSentEvent;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Flux;
//
//import java.util.List;
//
/// **
// * ChatController 提供与 AI 聊天的所有接口：
// * - 单轮问答
// * - 多轮对话（带上下文）
// * - 流式响应（SSE）
// * - 聊天历史获取与清除
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/chat")
//@RequiredArgsConstructor
//public class ChatController {
//
//    private final ChatClient chatClient;
//
//    // 注入 Redis 实现的 ChatMemory
//    @Qualifier("chatRedisMemory")
//    private final ChatMemory chatMemory;
//
//    /**
//     * 简单单轮问答接口（无上下文）
//     *
//     * @param text 用户输入
//     * @return AI 的响应文本
//     */
//    @PostMapping("/ask")
//    public String ask(@RequestParam String text) {
//        log.info("[ask] 用户提问：{}", text);
//        return chatClient.prompt().user(text).call().content().trim();
//    }
//
//    /**
//     * 多轮对话接口（携带 Redis 上下文）
//     *
//     * @param conversationId 会话标识（用于关联 Redis 存储）
//     * @param text           用户输入
//     * @return AI 响应文本
//     */
//    @PostMapping("/ai/stream")
//    public Flux<ServerSentEvent<String>> chat(@RequestParam String conversationId, @RequestParam(value = "text", defaultValue = "Hello!") String text) {
//
//        log.info("[chat] 会话 {} 用户输入：{}", conversationId, text);
//
//        return chatClient.prompt(text)
//                .advisors(new MessageChatMemoryAdvisor(chatMemory, conversationId, 10), new SimpleLoggerAdvisor())
//                .stream().content().map(content -> ServerSentEvent.builder(content).event("message").build())
//                //问题回答结速标识,以便前端消息展示处理
//                .concatWithValues(ServerSentEvent.builder("").build())
//                .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Error: " + e.getMessage()).event("error").build()));
//    }
//
//
//
//
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
//}
