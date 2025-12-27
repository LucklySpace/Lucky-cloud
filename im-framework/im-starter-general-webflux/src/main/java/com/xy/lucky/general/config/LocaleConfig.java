package com.xy.lucky.general.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;


/**
 * 国际化配置类（Locale & MessageSource）
 * <p>
 * 用于支持基于请求头（Accept-Language）或自定义逻辑的语言切换。
 * 默认加载路径：classpath:i18n/messages_*.properties
 * 如：messages_en.properties, messages_zh_CN.properties
 */
@Slf4j
@Configuration
public class LocaleConfig {

    /**
     * 设置默认时区为UTC
     */
    @PostConstruct
    void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        log.info("Set default timezone to: {}", ZoneOffset.UTC);
    }

    /**
     * Locale 解析器：根据请求头 Accept-Language 自动识别语言
     * 默认语言为英文（en）
     */
    @Bean
    @ConditionalOnMissingBean(LocaleContextResolver.class)
    public LocaleContextResolver localeResolver() {
        AcceptHeaderLocaleContextResolver resolver = new AcceptHeaderLocaleContextResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(java.util.List.of(
                Locale.ENGLISH,
                Locale.SIMPLIFIED_CHINESE,
                Locale.US,
                Locale.CHINA
        ));
        return resolver;
    }

    /**
     * 消息源配置：指定国际化资源文件路径和编码
     * 使用 ResourceBundleMessageSource 替代 ReloadableResourceBundleMessageSource 提高性能
     * 在生产环境中禁用重新加载以提高性能
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        // 生产环境中设置为0（禁用重新加载），开发环境中可以适当调整
        source.setCacheSeconds(0);
        return source;
    }
}
