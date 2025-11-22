package com.xy.lucky.spring.event;


/**
 * 定义事件发布接口
 */
public interface ApplicationEventPublisher {
    void publishEvent(Object event);
}
