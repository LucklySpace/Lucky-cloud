package com.xy.meet.entity;

import io.netty.channel.Channel;

public class User {

    private String userId;

    private Channel channel; // 用户对应的Netty Channel

    public User(String userId, Channel channel) {
        this.userId = userId;
        this.channel = channel;
    }

    public String getUserId() {
        return userId;
    }

    public Channel getChannel() {
        return channel;
    }
}