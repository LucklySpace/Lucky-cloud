package com.xy.lucky.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class ChatConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfig.class);


    /**
     * 内存型 ChatMemory Bean
     */
    @Bean("inMemoryMemory")
    public ChatMemory inMemoryMemory() {
        logger.info("Initializing InMemoryChatMemory");
        return new InMemoryChatMemory();
    }

    /**
     * 配置 ChatClient Bean，并添加日志记录
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            PgVectorStore vectorStore,
            ToolCallbackProvider toolCallbackProvider,
            @Qualifier("chatPostgresMemory") ChatMemory chatMemory
    ) {
        String systemPrompt = "你是一个智能机器人, 你的名字叫 Spring AI智能机器人";
        logger.info("Setting up ChatClient with system prompt: {}", systemPrompt);

        List<Advisor> advisors = List.of(
                // RAG 检索
                new QuestionAnswerAdvisor(vectorStore),
                // 短期对话记忆
                new MessageChatMemoryAdvisor(chatMemory),
                // 敏感词拦截
                SafeGuardAdvisor.builder()
                        .sensitiveWords(List.of("色情", "暴力"))
                        .order(2)
                        .failureResponse("抱歉，我无法回答这个问题。")
                        .build(),
                // 日志
                new SimpleLoggerAdvisor()
        );
        advisors.forEach(a -> logger.info("Loaded Advisor: {} (order={})", a.getName(), a.getOrder()));

        ChatClient client = builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(advisors)
                // 注册所有通过 ToolCallbackProvider 提供的工具
                .defaultTools(toolCallbackProvider.getToolCallbacks())
                .defaultOptions(ChatOptions.builder()
                        .topP(0.7)
                        .build())
                .build();

        logger.info("ChatClient created successfully with {} advisors", advisors.size());
        return client;
    }
}
