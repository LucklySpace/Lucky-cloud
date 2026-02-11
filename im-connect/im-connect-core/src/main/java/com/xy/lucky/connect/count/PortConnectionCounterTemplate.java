package com.xy.lucky.connect.count;


import com.xy.lucky.connect.domain.CountEvent;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.event.EventListener;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多端口连接计数器
 * 每个监听端口维护自己的连接数
 */
@Component
public class PortConnectionCounterTemplate {

    // 端口号 -> 连接数映射
    private static final ConcurrentHashMap<Integer, AtomicInteger> PORT_CONNECTION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立时调用，端口计数 +1
     */
    public static void increment(Channel ctx) {
        int port = ((InetSocketAddress) ctx.localAddress()).getPort();
        PORT_CONNECTION_MAP
                .computeIfAbsent(port, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * 连接关闭时调用，端口计数 -1
     */
    public static void decrement(Channel ctx) {
        int port = ((InetSocketAddress) ctx.localAddress()).getPort();
        AtomicInteger counter = PORT_CONNECTION_MAP.get(port);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }

    /**
     * 获取指定端口的当前连接数
     */
    public static int getConnectionCount(int port) {
        AtomicInteger counter = PORT_CONNECTION_MAP.get(port);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 获取所有端口的连接情况（只读）
     */
    public static ConcurrentHashMap<Integer, AtomicInteger> getAllConnectionCounts() {
        return new ConcurrentHashMap<>(PORT_CONNECTION_MAP);
    }

    /**
     * 监听消息
     *
     * @param countEvent
     */
    @EventListener(CountEvent.class)
    public void count(CountEvent countEvent) {
        if (countEvent.getFlag() == 1) {
            increment(countEvent.getCtx());
        } else {
            decrement(countEvent.getCtx());
        }
    }
}
