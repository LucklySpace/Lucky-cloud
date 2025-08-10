//package com.xy.ai.controller;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerSentEvent;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//import reactor.core.publisher.Flux;
//
//@RestController
//@CrossOrigin("*")
//@Slf4j
//public class ChatbotController {
//
//    private final ChatClient chatClient;
//
//    public ChatbotController(ChatClient chatClient) {
//        this.chatClient = chatClient;
//    }
//
//    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
//        //用户id
//        //String userId = request.userId();
//        return chatClient.prompt(request.message())
//                .stream().content().map(content -> ServerSentEvent.builder(content).event("message").build())
//                //问题回答结速标识,以便前端消息展示处理
//                .concatWithValues(ServerSentEvent.builder("[DONE]").build())
//                .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Error: " + e.getMessage()).event("error").build()));
//    }
//
//    record ChatRequest(String userId, String message) {
//
//    }
//}
//
