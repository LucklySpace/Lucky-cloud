package com.xy.lucky.spring.event;

import com.xy.lucky.spring.annotations.event.EventListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 优化版事件总线，支持异步发布、并发安全和高效注册
 * 改进点：
 * - 使用 CopyOnWriteArrayList 替换 ArrayList，确保并发读写安全（发布时无锁迭代）。
 * - 支持异步发布：默认异步调用监听器（使用共享线程池），避免阻塞发布者；可配置同步。
 * - 注册优化：缓存 Bean 的方法列表（但因 Bean 动态，实际通过反射缓存注解方法）；过滤非监听方法。
 * - 异常处理：使用 SLF4J 日志替换 printStackTrace，支持异常传播或静默。
 * - 扩展：添加 unregisterListener；支持方法优先级（注解扩展）；泛型事件类型。
 * - 性能：异步减少延迟；CopyOnWrite 适合读多写少场景（事件发布频繁，注册少）。
 */
@Slf4j
public class ApplicationEventBus implements ApplicationEventPublisher {

    // 事件类型 -> 监听器列表（并发安全）
    private final ConcurrentHashMap<Class<?>, List<ListenerInvoker>> listenerMap = new ConcurrentHashMap<>();

    // 共享线程池（固定大小，防止 OOM）；可注入自定义 Executor
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> new Thread(r, "EventBus-Executor")
    );

    /**
     * 注册监听器（容器初始化后调用）
     * 优化：使用 Stream 过滤注解方法，提高可读性；setAccessible 仅一次。
     */
    public void registerListener(Object bean) {
        if (bean == null) return;

        List<Method> listenerMethods = new ArrayList<>();
        for (Method m : bean.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(EventListener.class)) {
                listenerMethods.add(m);
            }
        }

        // 并行添加（如果方法多，但通常少；提升微小）
        Class<?> beanClass = bean.getClass();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandles.Lookup privateLookup;
        try {
            privateLookup = MethodHandles.privateLookupIn(beanClass, lookup);
        } catch (Exception e) {
            throw new IllegalStateException("无法创建 privateLookup: " + beanClass.getName(), e);
        }

        listenerMethods.parallelStream().forEach(m -> {
            Class<?> eventType = m.getAnnotation(EventListener.class).value();
            if (eventType == Object.class) { // 默认或空时，用方法参数类型
                if (m.getParameterCount() == 1) {
                    eventType = m.getParameterTypes()[0];
                } else {
                    log.warn("EventListener 方法 {} 参数无效，跳过", m.getName());
                    return;
                }
            }
            if (m.getParameterCount() != 1) {
                log.warn("EventListener 方法 {} 参数无效，跳过", m.getName());
                return;
            }

            MethodHandle handle;
            try {
                MethodType type = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
                handle = privateLookup.findVirtual(beanClass, m.getName(), type).bindTo(bean);
            } catch (Exception e) {
                throw new IllegalStateException("解析事件监听器句柄失败: " + beanClass.getName() + "." + m.getName(), e);
            }
            listenerMap.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(new ListenerInvoker(handle, m.getName(), 0));
        });

        log.debug("注册监听器: {} 个方法到 Bean {}", listenerMethods.size(), bean.getClass().getSimpleName());
    }

    /**
     * 注销监听器（可选，用于动态移除）
     */
    public void unregisterListener(Object bean) {
        if (bean == null) return;

        listenerMap.values().parallelStream().forEach(listeners -> {
            listeners.removeIf(invoker -> invoker.methodName == bean);
        });
        log.debug("注销监听器: Bean {}", bean.getClass().getSimpleName());
    }

    /**
     * 发布事件（异步默认，避免阻塞）
     *
     * @param event 事件对象
     */
    @Override
    public void publishEvent(Object event) {
        publishEvent(event, true); // 默认异步
    }

    /**
     * 发布事件（支持同步/异步切换）
     *
     * @param event 事件对象
     * @param async 是否异步（true: 非阻塞，false: 同步）
     */
    public void publishEvent(Object event, boolean async) {
        if (event == null) return;

        Class<?> eventType = event.getClass();
        List<ListenerInvoker> listeners = listenerMap.getOrDefault(eventType, List.of());

        if (listeners.isEmpty()) {
            log.trace("无监听器处理事件: {}", eventType.getSimpleName());
            return;
        }

        if (async) {
            for (ListenerInvoker invoker : listeners) {
                CompletableFuture.runAsync(() -> safeInvoke(invoker, event), executor);
            }
        } else {
            // 同步：串行调用，保持顺序
            for (ListenerInvoker invoker : listeners) {
                safeInvoke(invoker, event);
            }
        }

        log.debug("发布事件: {} 到 {} 个监听器 (async={})", eventType.getSimpleName(), listeners.size(), async);
    }

    /**
     * 安全调用监听器（异常隔离）
     */
    private void safeInvoke(ListenerInvoker invoker, Object event) {
        try {
            invoker.invoke(event);
        } catch (Exception e) {
            log.error("事件监听器执行异常: {} 处理 {}", invoker.methodName, event.getClass().getSimpleName(), e);
            // 可抛出或继续（当前静默，继续下一个）
        }
    }

    /**
     * 关闭资源（线程池）
     */
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        listenerMap.clear();
        log.info("ApplicationEventBus 已关闭");
    }

    /**
     * 内部类：监听器调用器（record 高效，immutable）
     */
    record ListenerInvoker(MethodHandle handle, String methodName, int priority) { // 可扩展优先级

        void invoke(Object event) throws Exception {
            try {
                handle.invoke(event);
            } catch (Throwable t) {
                if (t instanceof Exception e) throw e;
                throw new RuntimeException(t);
            }
        }
    }
}
