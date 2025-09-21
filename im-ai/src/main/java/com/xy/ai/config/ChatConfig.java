package com.xy.ai.config;

import com.xy.ai.advisor.ReReadingAdvisor;
import com.xy.ai.memory.ChatPostgresMemory;
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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;


@Configuration
public class ChatConfig {

    private static final Logger logger = LoggerFactory.getLogger(ChatConfig.class);

    @Bean
    public PgVectorStore pgVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        logger.info("Initializing PgVectorStore");
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .build();
    }

    /**
     * 内存型 ChatMemory Bean
     */
    @Bean("inMemoryMemory")
    public ChatMemory inMemoryMemory() {
        logger.info("Initializing InMemoryChatMemory");
        return new InMemoryChatMemory();
    }

    /**
     * postgres 存储 ChatMemory Bean
     */
    @Bean("postgresMemory")
    public ChatMemory postgresMemory() {
        logger.info("Initializing ChatPostgresMemory");
        return new ChatPostgresMemory();
    }

    /**
     * 配置 ChatClient Bean，并添加日志记录
     * https://blog.csdn.net/cjdlx123456789/article/details/144133837?fromshare=blogdetail&sharetype=blogdetail&sharerId=144133837&sharerefer=PC&sharesource=weixin_45357745&sharefrom=from_link
     * <p>
     * MessageChatMemoryAdvisor 主要功能是将用户提出的问题和模型的回答添加到历史记录中，从而形成一个上下文记忆的增强机制
     * 需要注意的是，并非所有的AI模型都支持这种上下文记忆的存储和管理方式。某些模型可能没有实现相应的历史记录功能，因此在使用MessageChatMemoryAdvisor时，确保所使用的模型具备此支持是至关重要的。
     * <p>
     * PromptChatMemoryAdvisor的功能在MessageChatMemoryAdvisor的基础上进一步增强，其主要作用在于上下文聊天记录的处理方式。与MessageChatMemoryAdvisor不同，PromptChatMemoryAdvisor并不将上下文记录直接传入messages参数中，而是巧妙地将其封装到systemPrompt提示词中。这一设计使得无论所使用的模型是否支持messages参数，系统都能够有效地增加上下文历史记忆。
     * <p>
     * QuestionAnswerAdvisor的主要功能是执行RAG（Retrieval-Augmented Generation）检索，这一过程涉及对知识库的高效调用。当用户提出问题时，QuestionAnswerAdvisor会首先对知识库进行检索，并将匹配到的相关引用文本添加到用户提问的后面，从而为生成的回答提供更为丰富和准确的上下文。
     * <p>
     * SafeGuardAdvisor的核心功能是进行敏感词校验，以确保系统在处理用户输入时的安全性和合规性。当用户提交的信息触发了敏感词机制，SafeGuardAdvisor将立即对该请求进行中途拦截，避免继续调用大型模型进行处理。
     * <p>
     * SimpleLoggerAdvisor：这是一个用于日志打印的工具，我们之前已经对其进行了练习和深入了解，因此在这里不再赘述。
     * <p>
     * VectorStoreChatMemoryAdvisor：该组件实现了长期记忆功能，能够将每次用户提出的问题及模型的回答存储到向量数据库中。
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            PgVectorStore vectorStore,
            ToolCallbackProvider toolCallbackProvider,
            @Qualifier("postgresMemory") ChatMemory chatMemory
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
                new SimpleLoggerAdvisor(),
                // 复读校验（示例）
                new ReReadingAdvisor()
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