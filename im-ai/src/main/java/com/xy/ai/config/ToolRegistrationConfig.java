package com.xy.ai.config;

import com.xy.ai.tools.time.DateTimeTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistrationConfig {

    /**
     * Spring 会自动注入容器中所有 Tool 实现类
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(new DateTimeTool())
                .build();
    }
}
