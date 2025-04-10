package com.xy.connect.netty.factory;

import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * 线程工厂
 */
public class NettyVirtualThreadFactory extends DefaultThreadFactory {

    /**
     * 线程工厂构造函数
     *
     * @param poolType 线程池类型
     * @param priority 线程数量
     */
    public NettyVirtualThreadFactory(Class<?> poolType, int priority) {
        super(poolType, priority);
    }

    /**
     * 创建虚拟线程
     *
     * @param r    线程
     * @param name 线程名称
     * @return 线程
     */
    @Override
    protected Thread newThread(Runnable r, String name) {
        // 直接使用 JDK 提供的虚拟线程创建 API
        return Thread.ofVirtual()
                .name("Virtual-Thread-" + name, 0)
                .unstarted(r);
    }
}