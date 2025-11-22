package com.xy.spring.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;

@ToString
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class BeanDefinition {


    /**
     * 方法
     */
    public Method factoryMethod;
    public Class FactoryBeanClass;
    /**
     * 类
     */
    public Object factoryBean;
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
        return this.factoryMethod != null;
    }
}