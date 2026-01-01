package com.xy.lucky.spring.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.invoke.MethodHandle;

@ToString
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class BeanDefinition {


    /**
     * 工厂方法句柄（@Bean 方法）
     */
    private MethodHandle factoryMethodHandle;
    private Class<?>[] factoryMethodParamTypes;
    private boolean factoryMethodStatic;
    private Class<?> factoryBeanClass;
    /**
     * 类
     */
    private Object factoryBean;
    /**
     * bean的名称
     */
    private String name;
    /**
     * bean的全类名
     */
    private String fullName;
    /**
     * bean的类型
     */
    private Class type;
    /**
     * bean的作用域
     */
    private String scope;
    /**
     * 是否懒加载
     */
    private boolean lazy;
    /**
     * 代理类型
     */
    private ProxyType proxyType;

    public boolean hasFactoryMethod() {
        return this.factoryMethodHandle != null;
    }
}
