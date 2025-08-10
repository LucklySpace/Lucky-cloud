//package com.xy.ai.controller;
//
//
//import com.xy.ai.memory.ChatRedisMemory;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.client.advisor.VectorStoreChatMemoryAdvisor;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * @Author majinzhong
// * @Date 2025/5/7 15:03
// * @Version 1.0
// */
//@CrossOrigin
//@RestController
//public class RedisAiController {
//
//    @Autowired
//    private ChatClient chatClient;
//
//    @Autowired
//    private ChatRedisMemory chatRedisMemory;
//
//
//    /**
//     * 持久化聊天记录
//     * @param message
//     * @param sessionId
//     * @return
//     */
//    @GetMapping("/ai/redisCall")
//    public String redisCall(@RequestParam(value = "message", defaultValue = "讲个笑话") String message, @RequestParam String sessionId) {
//        return chatClient.prompt().user(message)
//                .advisors(new MessageChatMemoryAdvisor(chatRedisMemory, sessionId, 10))
//                .call().content().trim();
//    }
//
//    @Autowired
//    VectorStore vectorStore;
//
//    /**
//     * 检索聊天记录向量数据库
//     * @param message
//     * @param sessionId
//     * @return
//     */
//    @GetMapping("/ai/vectorCall")
//    public String vectorCall(@RequestParam(value = "message", defaultValue = "讲个笑话") String message, @RequestParam String sessionId) {
//        VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore).build();
//        return chatClient.prompt().user(message)
//                .advisors(vectorStoreChatMemoryAdvisor)
//                .call().content().trim();
//    }
//}