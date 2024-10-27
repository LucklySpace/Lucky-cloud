package com.xy.connect.message.channels;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储用户channel
 */
public class UserChannelCtxMap {

    /*
     * 维护userId和ctx的关联关系，格式:Map<userId,ctx>
     */
    private static Map<String, ChannelHandlerContext> channelMap = new ConcurrentHashMap();

    /**
     * 添加channel
     * @param userId 用户id
     * @param ctx channel
     */
    public static void addChannel(String userId, ChannelHandlerContext ctx) {
        channelMap.put(userId, ctx);
    }

    /**
     * 移除channel
     * @param userId 用户id
     */
    public static void removeChannel(String userId) {
        if (userId != null) {
            channelMap.remove(userId);
        }
    }

    /**
     * 获取channel
     * @Param userId 用户id
     * @return channel
     */
    public static ChannelHandlerContext getChannel(String userId) {
        if (userId == null) {
            return null;
        }
        return channelMap.get(userId);
    }


    /**
     * 获取所有channel
     * @return 所有channel
     */
    public static Map<String, ChannelHandlerContext> getAllChannel() {
        return channelMap;
    }

}
