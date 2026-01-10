package com.xy.lucky.spring.boot.context;

/**
 * NoSuchBeanDefinitionException - Bean 不存在异常
 */
public class NoSuchBeanDefinitionException extends RuntimeException {

    private final String beanName;
    private final Class<?> beanType;

    public NoSuchBeanDefinitionException(String name) {
        super("No bean named '" + name + "' available");
        this.beanName = name;
        this.beanType = null;
    }

    public NoSuchBeanDefinitionException(Class<?> type) {
        super("No qualifying bean of type '" + type.getName() + "' available");
        this.beanName = null;
        this.beanType = type;
    }

    public NoSuchBeanDefinitionException(String name, String message) {
        super("No bean named '" + name + "' available: " + message);
        this.beanName = name;
        this.beanType = null;
    }

    public NoSuchBeanDefinitionException(Class<?> type, String message) {
        super("No qualifying bean of type '" + type.getName() + "' available: " + message);
        this.beanName = null;
        this.beanType = type;
    }

    public String getBeanName() {
        return this.beanName;
    }

    public Class<?> getBeanType() {
        return this.beanType;
    }
}

