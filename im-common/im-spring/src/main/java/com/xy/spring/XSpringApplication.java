package com.xy.spring;

import com.xy.spring.context.ApplicationContext;


public class XSpringApplication {
    private static ApplicationContext<?> context;

    public static ApplicationContext<?> getContext() {
        if (context == null) {
            throw new IllegalStateException("ApplicationContext 尚未初始化");
        }
        return context;
    }

    public static void setContext(ApplicationContext<?> ctx) {
        context = ctx;
    }

    public static void run(Class<?> startupClass, String[] args) {
        ApplicationContext applicationContext = new ApplicationContext(startupClass, args);
        if (context == null) {
            setContext(applicationContext);
        }
    }
}