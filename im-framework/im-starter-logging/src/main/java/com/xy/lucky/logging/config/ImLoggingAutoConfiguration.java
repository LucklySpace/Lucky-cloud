package com.xy.lucky.logging.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.appender.HttpLogAppender;
import com.xy.lucky.logging.sender.AsyncLogSender;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class ImLoggingAutoConfiguration {

    @Bean
    public LoggingProperties loggingProperties(Environment env) {

        LoggingProperties props = new LoggingProperties();

        Binder.get(env).bind("im.logging", Bindable.ofInstance(props));

        if (props.getService() == null || props.getService().isBlank()) {
            props.setService(env.getProperty("spring.application.name", "unknown"));
        }
        if (props.getEnv() == null || props.getEnv().isBlank()) {
            props.setEnv(env.getProperty("spring.profiles.active", env.getProperty("app.env", "dev")));
        }

        return props;
    }

    @Bean
    public AsyncLogSender asyncLogSender(LoggingProperties props, ObjectProvider<ObjectMapper> om) {
        ObjectMapper objectMapper = om.getIfAvailable(ObjectMapper::new);
        AsyncLogSender sender = new AsyncLogSender(props, objectMapper);
        if (props.isEnabled()) {
            sender.start();
        }
        return sender;
    }

    @Bean
    public HttpLogAppender httpLogAppender(LoggingProperties props, AsyncLogSender sender) {

        HttpLogAppender appender = new HttpLogAppender(props, sender);

        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 设置全局属性供 XML 使用 (可选)
        ctx.putProperty("APP_NAME", props.getService());

        if (props.isEnabled() && props.isAttachToRootLogger()) {
            Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
            if (root.getAppender("HttpLogAppender") == null) {
                appender.setContext(ctx);
                appender.setName("HttpLogAppender");
                appender.start();
                root.addAppender(appender);
            }
        }
        return appender;
    }
}
