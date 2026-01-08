package com.xy.lucky.live.server;

import com.xy.lucky.live.signal.SignalDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket 消息处理器
 * <p>
 * 处理 WebSocket 帧消息，包括文本消息、心跳检测等
 * 将消息转发给 SignalDispatcher 进行业务处理
 *
 * @author lucky
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    private final SignalDispatcher signalDispatcher;

    public WebSocketHandler(SignalDispatcher signalDispatcher) {
        this.signalDispatcher = signalDispatcher;
    }

    /**
     * Channel 激活时调用
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("WebSocket 连接建立: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    /**
     * Channel 失活时调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("WebSocket 连接断开: {}", ctx.channel().remoteAddress());
        // 通知 SignalDispatcher 处理断开连接
        signalDispatcher.handleDisconnect(ctx);
        super.channelInactive(ctx);
    }

    /**
     * 处理接收到的 WebSocket 帧
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame textFrame) {
            String text = textFrame.text();
            log.debug("收到消息: {}", text);
            // 分发消息到 SignalDispatcher
            signalDispatcher.dispatch(ctx, text);
        } else {
            // 不支持的帧类型
            log.warn("不支持的 WebSocket 帧类型: {}", frame.getClass().getSimpleName());
        }
    }

    /**
     * 处理用户事件（空闲检测等）
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent) {
            switch (idleEvent.state()) {
                case READER_IDLE -> {
                    // 读空闲，客户端可能已断开
                    log.info("客户端读空闲，关闭连接: {}", ctx.channel().remoteAddress());
                    ctx.close();
                }
                case WRITER_IDLE -> {
                    // 写空闲，可以发送心跳（如果需要）
                    log.debug("写空闲: {}", ctx.channel().remoteAddress());
                }
                case ALL_IDLE -> {
                    // 读写都空闲
                    log.info("客户端完全空闲，关闭连接: {}", ctx.channel().remoteAddress());
                    ctx.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebSocket 处理异常: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }
}

