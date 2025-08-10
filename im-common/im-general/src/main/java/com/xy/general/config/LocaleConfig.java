package com.xy.general.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

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
     * 设置默认时区
     */
    @PostConstruct
    void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
        log.warn("set default timezone:{}", ZoneOffset.UTC);
        //TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        //TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }

    /**
     * Locale 解析器：根据请求头 Accept-Language 自动识别语言
     * 默认语言为英文（en）
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    /**
     * 消息源配置：指定国际化资源文件路径和编码
     * 支持热更新（缓存时间可配置）
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages"); // 对应 messages_zh_CN.properties 等
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true); // 找不到 key 时直接使用 key
        return source;
    }

    /*
     * ✅ 可选：自定义 Locale 解析逻辑（优先从请求头中获取语言，支持 fallback）
     *
     * 使用此方案可对 Accept-Language 值进行更精细控制（如中文则强制中文，默认英文）
     *
     * 取消注释以下 Bean 可覆盖默认 localeResolver：
     */
//    @Bean
//    public LocaleResolver customLocaleResolver() {
//        return new AcceptHeaderLocaleResolver() {
//            @Override
//            public Locale resolveLocale(HttpServletRequest request) {
//                String lang = request.getHeader("Accept-Language");
//                if (!StringUtils.hasText(lang)) {
//                    return Locale.ENGLISH;
//                }
//                Locale locale = Locale.forLanguageTag(lang);
//                return locale.getLanguage().equals("zh") ? Locale.SIMPLIFIED_CHINESE : locale;
//            }
//        };
//    }

}
