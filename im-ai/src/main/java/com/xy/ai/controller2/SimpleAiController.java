//package com.xy.ai.controller;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.memory.InMemoryChatMemory;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerSentEvent;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Flux;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * @Author majinzhong
// * @Date 2025/4/28 10:37
// * @Version 1.0
// * SpringAI对话样例
// */
//@CrossOrigin
//@RestController
//public class SimpleAiController {
//
//    @Autowired
//    VectorStore vectorStore;
//    // 负责处理OpenAI的bean，所需参数来自properties文件
//    private final ChatClient chatClient;
//    //对话记忆
//    private final InMemoryChatMemory inMemoryChatMemory;
//
//    public SimpleAiController(ChatClient chatClient,InMemoryChatMemory inMemoryChatMemory) {
//        this.chatClient = chatClient;
//        this.inMemoryChatMemory = inMemoryChatMemory;
//    }
//
//    /**
//     * 根据消息直接输出回答
//     * @param map
//     * @return
//     */
//    @PostMapping("/ai/call")
//    public String call(@RequestBody Map<String,String> map) {
//        String message = map.get("message");
//        return chatClient.prompt().user(message).call().content().trim();
//    }
//
//    /**
//     * 根据消息采用流式输出
//     * @param message
//     * @return
//     */
//    @PostMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<ServerSentEvent<String>> streamChat(@RequestParam(value = "message", defaultValue = "Hello!") String message) {
//        return chatClient.prompt(message)
//                .stream().content().map(content -> ServerSentEvent.builder(content).event("message").build())
//                //问题回答结速标识,以便前端消息展示处理
//                .concatWithValues(ServerSentEvent.builder("").build())
//                .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Error: " + e.getMessage()).event("error").build()));
//    }
//
//    /**
//     * 对话记忆（多轮对话）
//     * @param message
//     * @return
//     * @throws InterruptedException
//     */
//    @GetMapping(value = "/ai/streamresp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<ServerSentEvent<String>> streamResp(@RequestParam(value = "message", defaultValue = "Hello!") String message){
//        Flux<ServerSentEvent<String>> serverSentEventFlux = chatClient.prompt(message)
//                .advisors(new MessageChatMemoryAdvisor(inMemoryChatMemory, "123", 10), new SimpleLoggerAdvisor())
//                .stream().content().map(content -> ServerSentEvent.builder(content).event("message").build())
//                //问题回答结速标识,以便前端消息展示处理
//                .concatWithValues(ServerSentEvent.builder("").build())
//                .onErrorResume(e -> Flux.just(ServerSentEvent.builder("Error: " + e.getMessage()).event("error").build()));
//        return serverSentEventFlux;
//    }
//
//    /**
//     * 整合知识库和自己提问的问题一块向AI提问
//     * @param message
//     * @return
//     */
//    @GetMapping("/ai/vectorStoreChat")
//    public Flux<String> ollamaApi(@RequestParam(value = "message") String message) {
//        //从知识库检索相关信息，再将检索得到的信息同用户的输入一起构建一个prompt，最后调用ollama api
//        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(1).build());
//        String targetMessage = String.format("已知信息：%s\n 用户提问：%s\n", documents.get(0).getText(), message);
//        return chatClient.prompt(targetMessage).stream().content();
//    }
//}