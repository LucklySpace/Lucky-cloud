package com.xy.lucky.spring.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.invoke.MethodHandle;

/**
 * BeanDefinition - Bean 定义
 * <p>
 * 保存 Bean 的元数据信息，包括类型、作用域、工厂方法等
 */
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

    /**
     * 工厂方法参数类型
     */
    private Class<?>[] factoryMethodParamTypes;

    /**
     * 工厂方法是否为静态
     */
    private boolean factoryMethodStatic;

    /**
     * 工厂 Bean 类（@Configuration 类）
     */
    private Class<?> factoryBeanClass;

    /**
     * 工厂 Bean 实例
     */
    private Object factoryBean;

    /**
     * Bean 名称
     */
    private String name;

    /**
     * Bean 全类名
     */
    private String fullName;

    /**
     * Bean 类型
     */
    private Class<?> type;
    
    /**
     * Bean 作用域（singleton/prototype）
     */
    private String scope;

    /**
     * 是否懒加载
     */
    private boolean lazy;

    /**
     * 判断是否有工厂方法
     */
    public boolean hasFactoryMethod() {
        return this.factoryMethodHandle != null;
    }
}
