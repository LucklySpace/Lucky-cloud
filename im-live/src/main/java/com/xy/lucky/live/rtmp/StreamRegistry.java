package com.xy.lucky.live.rtmp;

import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流注册中心（内存版）
 * - 管理推流者与拉流者
 * - 后续可替换为分布式注册（如 Redis / gRPC）
 */
@Component
public class StreamRegistry {
    private final Map<String, Channel> publishers = new ConcurrentHashMap<>();
    private final Map<String, ChannelGroup> subscribers = new ConcurrentHashMap<>();

    private String key(String app, String stream) {
        return app + "/" + stream;
    }

    public void publish(String app, String stream, Channel ch) {
        publishers.put(key(app, stream), ch);
    }

    public void play(String app, String stream, Channel ch) {
        String k = key(app, stream);
        ChannelGroup group = subscribers.computeIfAbsent(k, s -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        group.add(ch);
    }

    public Channel getPublisher(String app, String stream) {
        return publishers.get(key(app, stream));
    }

    public ChannelGroup getSubscribers(String app, String stream) {
        return subscribers.get(key(app, stream));
    }
}

