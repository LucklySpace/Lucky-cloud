package com.xy.connect.netty.factory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * EpollEventLoopGroup 与 NioEventLoopGroup 的使用区别以及出现场景
 * 在linux上使用EpollEventLoopGroup会有较少的gc有更高级的特性，只有在Linux上才可以使用
 * 文章链接：http://li5jun.com/article/391.html
 */
public class NettyEventLoopFactory {

    private static final String NETTY_EPOLL_ENABLE_KEY = "netty.epoll.enable";
    private static final String OS_NAME_KEY = "os.name";
    private static final String OS_LINUX_PREFIX = "linux";

    private static final boolean isEpollEnabled;
    private static final boolean isLinux;

    static {
        String osName = System.getProperty(OS_NAME_KEY);
        isLinux = osName != null && osName.toLowerCase().contains(OS_LINUX_PREFIX);

        String epollEnabled = System.getProperty(NETTY_EPOLL_ENABLE_KEY, "false");
        isEpollEnabled = Boolean.parseBoolean(epollEnabled) && isLinux && Epoll.isAvailable();
    }

    public static EventLoopGroup eventLoopGroup(int threads) {
        return isEpollEnabled ? new EpollEventLoopGroup(threads) : new NioEventLoopGroup(threads);
    }

    public static Class<? extends ServerSocketChannel> serverSocketChannelClass() {
        return isEpollEnabled ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }
}
