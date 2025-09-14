package com.xy.connect.netty.factory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class NettyEventLoopFactory {

    private static final String NETTY_EPOLL_ENABLE_KEY = "netty.epoll.enable";
    private static final String OS_NAME_KEY = "os.name";
    private static final String OS_LINUX_PREFIX = "linux";

    // EPOLL 是否已启用
    private static final boolean IS_EPOLL_ENABLED;

    // 是否linux
    private static final boolean IS_LINUX;

    static {
        String osName = System.getProperty(OS_NAME_KEY);
        IS_LINUX = osName != null && osName.toLowerCase().contains(OS_LINUX_PREFIX);

        String epollEnabled = System.getProperty(NETTY_EPOLL_ENABLE_KEY, "false");
        IS_EPOLL_ENABLED = Boolean.parseBoolean(epollEnabled) && IS_LINUX && Epoll.isAvailable();
    }

    /**
     * 创建 EventLoopGroup，内部线程使用虚拟线程创建（通过 VirtualThreadFactory）。
     *
     * @param threads 线程数
     * @return EventLoopGroup 实例
     */
    public static EventLoopGroup eventLoopGroup(int threads) {
        // 使用自定义的 VirtualThreadFactory 来构造 EventLoopGroup
        ThreadFactory threadFactory = new NettyVirtualThreadFactory(
                IS_EPOLL_ENABLED ? EpollEventLoopGroup.class : NioEventLoopGroup.class,
                Thread.MAX_PRIORITY
        );
        return IS_EPOLL_ENABLED
                ? new EpollEventLoopGroup(threads, threadFactory)
                : new NioEventLoopGroup(threads, threadFactory);
    }


    /**
     * 根据当前环境选择合适的 ServerSocketChannel 类。
     *
     * @return ServerSocketChannel 的 Class 对象
     */
    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return IS_EPOLL_ENABLED ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }
}
