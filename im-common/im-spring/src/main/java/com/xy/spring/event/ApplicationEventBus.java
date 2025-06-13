package com.xy.spring.event;

import com.xy.spring.annotations.event.EventListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件注册监听
 */
@Slf4j
public class ApplicationEventBus implements ApplicationEventPublisher {

    // 监听类集合
    private final Map<Class<?>, List<ListenerInvoker>> listenerMap = new ConcurrentHashMap<>();

    /**
     * 注册监听器方法（在容器初始化后调用）
     *
     * @param bean
     */
    public void registerListener(Object bean) {

        // 遍历bean中方法上是否含有 EventListener注解
        for (Method m : bean.getClass().getDeclaredMethods()) {

            if (!m.isAnnotationPresent(EventListener.class)) continue;

            Class<?> eventType = m.getAnnotation(EventListener.class).value();

            m.setAccessible(true);

            listenerMap.computeIfAbsent(eventType, k -> new ArrayList<>())
                    .add(new ListenerInvoker(bean, m));
        }
    }

    /**
     * 发布事件
     *
     * @param event 事件类
     */
    @Override
    public void publishEvent(Object event) {
        List<ListenerInvoker> listeners = listenerMap.get(event.getClass());

        if (listeners != null) {
            for (ListenerInvoker invoker : listeners) {
                try {
                    invoker.invoke(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 内部类：封装监听器方法
     *
     * @param bean   类
     * @param method 方法
     */
    record ListenerInvoker(Object bean, Method method) {

        void invoke(Object event) throws Exception {
            method.invoke(bean, event);
        }
    }
}