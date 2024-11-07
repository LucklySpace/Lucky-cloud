package com.xy.meet.netty;


import com.xy.meet.entity.Message;
import com.xy.meet.entity.Room;
import com.xy.meet.entity.User;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IMeetChatServerHandler extends SimpleChannelInboundHandler<Message> {

    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        handleMessage(ctx, msg);
    }

    private void handleMessage(ChannelHandlerContext ctx, Message message) {
        String type = message.getType();
        String roomId = message.getRoomId();
        String userId = message.getUserId();

        switch (type) {
            case "join":
                joinRoom(ctx, roomId, userId);
                break;
            case "leave":
                leaveRoom(ctx, roomId, userId);
                break;
            case "signal":
                forwardSignal(roomId, message);
                break;
            case "heartbeat":
                sendHeartbeatAck(ctx, roomId, userId);
                break;
            default:
                sendUnknownMessageType(ctx, type);
                break;
        }
    }

    private void joinRoom(ChannelHandlerContext ctx, String roomId, String userId) {
        Room room = rooms.computeIfAbsent(roomId, Room::new);
        User user = new User(userId, ctx.channel());
        room.addUser(user);

        log.info("用户 {} 加入房间 {}", userId, roomId);

        Message joinMessage = createRoomMessage("join", roomId, userId, "joined", room);
        // 广播加入消息
        broadcastToRoom(room, joinMessage, userId, true);
    }

    private void leaveRoom(ChannelHandlerContext ctx, String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            // 移除用户
            room.removeUser(userId);
            log.info("用户 {} 离开房间 {}", userId, roomId);
            // 创建离开消息
            Message leaveMessage = createRoomMessage("leave", roomId, userId, "leaved", room);
            // 广播离开消息
            broadcastToRoom(room, leaveMessage, userId, false);

            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    private void forwardSignal(String roomId, Message message) {
        Room room = rooms.get(roomId);
        if (room != null) {
            log.info("房间 {} 用户 {} 发送消息:{} ", roomId, message.getUserId(), message.getBody());
            room.getUsers().forEach(user -> user.getChannel().writeAndFlush(message));
        }
    }

    /**
     * 处理心跳消息
     *
     * @param ctx
     * @param roomId
     * @param userId
     */
    private void sendHeartbeatAck(ChannelHandlerContext ctx, String roomId, String userId) {
        Room room = rooms.get(roomId);

        log.info("用户 {} 心跳中 房间 {}", userId, roomId);

        Message heartbeatMessage = createRoomMessage("heartbeat", roomId, userId, "heartbeat_ack", room);
        // 发送心跳响应
        ctx.writeAndFlush(heartbeatMessage);
    }

    /**
     * 处理未知消息类型
     *
     * @param ctx
     * @param type
     */
    private void sendUnknownMessageType(ChannelHandlerContext ctx, String type) {
        Message errorMessage = new Message();

        errorMessage.setBody("Unknown message type: " + type);

        ctx.writeAndFlush(errorMessage);
    }

    /**
     * 创建房间消息
     *
     * @param type   消息类型
     * @param roomId 房间ID
     * @param userId 用户ID
     * @param body   消息内容
     * @param room   房间信息
     * @return 消息对象
     */
    private Message createRoomMessage(String type, String roomId, String userId, String body, Room room) {
        Message message = new Message();
        message.setType(type);
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setUsers(room.getUsers());
        message.setBody(body);
        return message;
    }

    /**
     * 向房间内的其他用户广播消息
     *
     * @param room          房间
     * @param message       房间消息
     * @param excludeUserId 排除的用户ID
     * @param dotFilter     是否不过滤排除的用户
     */
    private void broadcastToRoom(Room room, Message message, String excludeUserId, boolean dotFilter) {
        room.getUsers().stream()
                .filter(user -> dotFilter || !user.getUserId().equals(excludeUserId))
                .forEach(user -> user.getChannel().writeAndFlush(message));
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        rooms.values().forEach(room -> room.getUsers().removeIf(user -> {
            if (user.getChannel().equals(ctx.channel())) {
                log.info("用户 {} 断开连接或心跳超时 离开房间 {}", user.getUserId(), room.getRoomId());
                broadcastToRoom(room, createRoomMessage("leave", room.getRoomId(), user.getUserId(), "leaved", room), user.getUserId(), false);
                return true;
            }
            return false;
        }));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent.state() == IdleState.ALL_IDLE) {
                handlerRemoved(ctx);
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

