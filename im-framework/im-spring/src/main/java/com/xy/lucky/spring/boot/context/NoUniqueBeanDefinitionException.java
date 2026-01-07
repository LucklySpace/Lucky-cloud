package com.xy.lucky.spring.boot.context;

import java.util.Collection;

/**
 * NoUniqueBeanDefinitionException - 存在多个匹配 Bean 的异常
 */
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

    private final int numberOfBeansFound;
    private final Collection<String> beanNamesFound;

    public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
        super(type, message);
        this.numberOfBeansFound = numberOfBeansFound;
        this.beanNamesFound = null;
    }

    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
        super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " + beanNamesFound);
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    public int getNumberOfBeansFound() {
        return this.numberOfBeansFound;
    }

    public Collection<String> getBeanNamesFound() {
        return this.beanNamesFound;
    }
}

