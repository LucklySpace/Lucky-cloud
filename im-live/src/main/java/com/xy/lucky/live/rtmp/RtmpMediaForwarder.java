package com.xy.lucky.live.rtmp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 媒体转发器：
 * - 对于推流者发送的音视频消息（typeId=8/9），转发至订阅者
 * - 命令消息透传给下一个处理器
 */
public class RtmpMediaForwarder extends SimpleChannelInboundHandler<RtmpMessage> {
    private static final Logger log = LoggerFactory.getLogger(RtmpMediaForwarder.class);
    private final StreamRegistry registry;
    private String appName = "live";
    private String streamName;

    public RtmpMediaForwarder(StreamRegistry registry) {
        super(false); // 不自动释放 ByteBuf，交由下游编码器处理
        this.registry = registry;
    }

    public void setContext(String app, String stream) {
        this.appName = app;
        this.streamName = stream;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtmpMessage msg) {
        int type = msg.messageTypeId;
        String appAttr = ctx.channel().attr(RtmpCommandHandler.ATTR_APP).get();
        String streamAttr = ctx.channel().attr(RtmpCommandHandler.ATTR_STREAM).get();
        if (appAttr != null) appName = appAttr;
        if (streamAttr != null) streamName = streamAttr;
        if (type == 8 || type == 9) {
            if (appName == null || streamName == null) {
                ctx.fireChannelRead(msg);
                return;
            }
            ChannelGroup subs = registry.getSubscribers(appName, streamName);
            if (subs != null && !subs.isEmpty()) {
                subs.writeAndFlush(msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("媒体转发异常", cause);
        ctx.close();
    }
}
