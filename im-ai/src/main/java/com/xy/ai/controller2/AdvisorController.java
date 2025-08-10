//package com.xy.ai.controller;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.memory.InMemoryChatMemory;
//import org.springframework.ai.chat.messages.Message;
//import org.springframework.ai.chat.messages.MessageType;
//import org.springframework.ai.chat.messages.UserMessage;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
//import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;
//
///**
// * @Author majinzhong
// * @Date 2025/5/6 14:22
// * @Version 1.0
// */
//@CrossOrigin
//@RestController
//public class AdvisorController {
//
//    // 负责处理OpenAI的bean，所需参数来自properties文件
//    private final ChatClient chatClient;
//    //对话记忆
//    private final InMemoryChatMemory inMemoryChatMemory;
//
//    @Autowired
//    public AdvisorController(ChatClient chatClient,InMemoryChatMemory inMemoryChatMemory) {
//        this.chatClient = chatClient;
//        this.inMemoryChatMemory = inMemoryChatMemory;
//    }
//
//    /**
//     * 普通聊天
//     * @param message
//     * @param sessionId
//     * @return
//     */
//    @GetMapping("/ai/generateCall")
//    public String generateCall(@RequestParam(value = "message", defaultValue = "讲个笑话") String message, @RequestParam String sessionId) {
//        return chatClient.prompt().user(message)
//                .advisors(advisorSpec -> advisorSpec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId).param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
//                .call().content().trim();
//    }
//
//    /**
//     * 根据消息直接输出回答
//     * @param map
//     * @return
//     */
//    @PostMapping("/ai/callAdvisor")
//    public String call(@RequestBody Map<String,String> map) {
//        String message = map.get("message");
//        Message message1 = new UserMessage(message);
//        String trim = chatClient.prompt().user(message).call().content().trim();
//        Message message2 = new UserMessage(MessageType.SYSTEM, trim, new ArrayList<>(), Map.of());
//        inMemoryChatMemory.add("123456",List.of(message1,message2));
//        return trim;
//    }
//
//    /**
//     * 查询聊天记里
//     * @return
//     */
//    @GetMapping("/ai/chatMemory")
//    public List<Message> chatMemory(){
//        List<Message> messages = inMemoryChatMemory.get("123456", 10);
//        for (Message message : messages) {
//            System.out.println(message.getText());
//        }
//        return messages;
//    }
//}