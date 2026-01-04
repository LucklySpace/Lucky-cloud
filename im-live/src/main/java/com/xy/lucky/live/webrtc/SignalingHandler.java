package com.xy.lucky.live.webrtc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文本帧处理器
 * 支持消息格式：
 * {
 * "type": "join|leave|offer|answer|candidate",
 * "roomId": "...",
 * "userId": "...",
 * "targetId": "...", // 目标用户，仅点对点消息需要
 * "payload": { ... } // SDP/ICE 数据
 * }
 */
public class SignalingHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger log = LoggerFactory.getLogger(SignalingHandler.class);

    private final RoomService roomService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final java.util.concurrent.ExecutorService executor;

    public SignalingHandler(RoomService roomService, java.util.concurrent.ExecutorService executor) {
        this.roomService = roomService;
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String text = msg.text();
        executor.execute(() -> {
            try {
                JsonNode root = mapper.readTree(text);
                String type = root.path("type").asText("");
                String roomId = root.path("roomId").asText("");
                String userId = root.path("userId").asText("");
                String targetId = root.path("targetId").asText("");
                JsonNode payload = root.path("payload");
                Channel ch = ctx.channel();

                switch (type) {
                    case "join" -> roomService.joinRoom(roomId, userId, ch);
                    case "leave" -> roomService.leaveRoom(roomId, userId);
                    case "offer", "answer", "candidate" -> roomService.forward(roomId, userId, targetId, type, payload);
                    default -> log.warn("未知消息类型: {}", type);
                }
            } catch (Exception e) {
                log.warn("信令处理异常", e);
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        roomService.cleanupByChannel(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("信令通道异常", cause);
        ctx.close();
    }
}
