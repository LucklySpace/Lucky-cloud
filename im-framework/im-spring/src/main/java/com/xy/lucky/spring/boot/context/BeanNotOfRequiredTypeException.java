package com.xy.lucky.spring.boot.context;

/**
 * BeanNotOfRequiredTypeException - Bean 类型不匹配异常
 */
public class BeanNotOfRequiredTypeException extends RuntimeException {

    private final String beanName;
    private final Class<?> requiredType;
    private final Class<?> actualType;

    public BeanNotOfRequiredTypeException(String beanName, Class<?> requiredType, Class<?> actualType) {
        super("Bean named '" + beanName + "' is expected to be of type '" + requiredType.getName() +
                "' but was actually of type '" + actualType.getName() + "'");
        this.beanName = beanName;
        this.requiredType = requiredType;
        this.actualType = actualType;
    }

    public String getBeanName() {
        return this.beanName;
    }

    public Class<?> getRequiredType() {
        return this.requiredType;
    }

    public Class<?> getActualType() {
        return this.actualType;
    }
}

