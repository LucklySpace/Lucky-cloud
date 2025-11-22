package com.xy.spring.core;

public interface DisposableBean {
    void destroy() throws Exception;
}