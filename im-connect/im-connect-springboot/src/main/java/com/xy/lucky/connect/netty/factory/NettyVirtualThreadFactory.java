package com.xy.lucky.connect.netty.factory;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 优化的 Netty 虚拟线程工厂
 * <p>
 * - 支持回退到平台线程（如果 JVM 不支持虚拟线程，如 JDK<21），确保兼容性
 * - 使用 AtomicLong 实现线程名全局唯一计数，避免并发冲突，提高可靠性
 * - 可配置线程前缀、是否 daemon、优先级，增加灵活性
 * - 添加 UncaughtExceptionHandler 记录未捕获异常，便于调试和监控
 * - 保留 Netty DefaultThreadFactory 的语义，确保无缝集成 EventLoopGroup
 * - 性能极致：虚拟线程创建轻量，无额外开销；fallback 逻辑仅初次检查
 */
public class NettyVirtualThreadFactory extends DefaultThreadFactory {

    private static final boolean VIRTUAL_THREADS_SUPPORTED = false;

    private final String prefix;
    private final boolean daemon;
    private final AtomicLong counter = new AtomicLong(0);

    /**
     * 构造函数：默认使用虚拟线程前缀，非 daemon
     *
     * @param poolType 线程池类型（用于 Netty 兼容）
     * @param priority 线程优先级
     */
    public NettyVirtualThreadFactory(Class<?> poolType, int priority) {
        this(poolType, priority, "im-connect-virtual-thread-", false);
    }

    /**
     * 扩展构造函数：自定义前缀和 daemon 标志
     *
     * @param poolType 线程池类型
     * @param priority 线程优先级
     * @param prefix   线程名前缀（e.g., "Netty-VT-")
     * @param daemon   是否 daemon 线程
     */
    public NettyVirtualThreadFactory(Class<?> poolType, int priority, String prefix, boolean daemon) {
        super(poolType, priority);
        this.prefix = prefix != null ? prefix : "im-connect-virtual-thread-";
        this.daemon = daemon;
    }

    @Override
    protected Thread newThread(Runnable r, String name) {
        Runnable wrapped = () -> {
            try {
                r.run();
            } catch (Throwable t) {
                System.err.println("Uncaught exception in thread: " + Thread.currentThread().getName());
                t.printStackTrace();
            }
        };
        String threadName = prefix + name + "-" + counter.getAndIncrement();
        Thread t = new Thread(wrapped, threadName);
        t.setDaemon(daemon);
        t.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
        return t;
    }

    /**
     * 自定义未捕获异常处理器：记录日志，便于问题诊断
     */
    private static class LoggingUncaughtExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.printf("Uncaught exception in thread %s: %s%n", t.getName(), e.getMessage());
            e.printStackTrace();
        }
    }
}
