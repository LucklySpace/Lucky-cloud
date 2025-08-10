package com.xy.general.response.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;


/**
 * 国际化消息服务工具类
 *
 * <p>提供静态方法访问消息资源，支持国际化(i18n)和参数化消息</p>
 *
 * @Component 表示这是一个Spring组件，会被自动扫描并纳入Spring容器管理
 */
@Component
public class I18nService {

    // 静态MessageSource实例，用于消息解析
    private static MessageSource messageSource;

    /**
     * 根据消息键获取当前语言环境下的消息
     *
     * <p>自动从LocaleContextHolder获取当前请求的语言环境</p>
     *
     * @param key 消息资源键
     * @return 对应语言环境下的消息文本
     * @see LocaleContextHolder
     */
    public static String getMessage(String key) {
        // 自动根据请求头解析当前语言环境
        Locale locale = LocaleContextHolder.getLocale();
        return getMessage(key, null, locale);
    }

    /**
     * 根据消息键和参数获取默认语言环境下的消息
     *
     * @param key  消息资源键
     * @param args 消息中的参数数组(可为null)
     * @return 对应语言环境下的消息文本(带参数替换)
     */
    public static String getMessage(String key, Object[] args) {
        return messageSource.getMessage(key, args, Locale.getDefault());
    }

    /**
     * 根据消息键、参数和指定语言环境获取消息
     *
     * @param key    消息资源键
     * @param args   消息中的参数数组(可为null)
     * @param locale 指定的语言环境
     * @return 指定语言环境下的消息文本(带参数替换)
     * @throws org.springframework.context.NoSuchMessageException 如果找不到指定键的消息
     */
    public static String getMessage(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * 注入MessageSource实例的setter方法
     *
     * <p>使用@Autowired实现依赖注入，@Qualifier指定bean名称</p>
     *
     * @param messageSource 要注入的MessageSource实例
     */
    @Autowired
    public void setMessageSource(@Qualifier("messageSource") MessageSource messageSource) {
        // 将注入的MessageSource实例赋值给静态变量
        I18nService.messageSource = messageSource;
    }
}