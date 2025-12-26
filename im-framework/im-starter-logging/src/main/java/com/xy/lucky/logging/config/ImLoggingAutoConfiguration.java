package com.xy.lucky.logging.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.logging.appender.HttpLogAppender;
import com.xy.lucky.logging.sender.AsyncLogSender;
import com.xy.lucky.logging.trace.TraceIdFilter;
import jakarta.servlet.Filter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class ImLoggingAutoConfiguration {

    @Bean
    public LoggingProperties loggingProperties(Environment env) {
        LoggingProperties props = new LoggingProperties();
        Binder binder = Binder.get(env);
        binder.bind("im.logging", Bindable.ofInstance(props));
        if (props.getModule() == null || props.getModule().isBlank()) {
            String appName = env.getProperty("spring.application.name", "unknown");
            props.setModule(appName);
        }
        if (props.getService() == null || props.getService().isBlank()) {
            props.setService(props.getModule());
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
        ctx.putProperty("APP_NAME", props.getModule());
        if (props.isEnabled() && props.isAttachToRootLogger()) {
            Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
            if (root.getAppender("HttpLogAppender") == null) {
                appender.setContext(ctx);
                appender.start();
                root.addAppender(appender);
            }
        }
        return appender;
    }

    @Bean
    public FilterRegistrationBean<Filter> traceIdFilter(LoggingProperties props) {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        Filter filter = new TraceIdFilter("X-Trace-Id", "X-Span-Id", props.getMdcTraceIdKey(), props.getMdcSpanIdKey());
        reg.setFilter(filter);
        reg.setOrder(0);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
