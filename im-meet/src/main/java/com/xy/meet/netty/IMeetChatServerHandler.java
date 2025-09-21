package com.xy.meet.netty;


import com.xy.meet.constant.Constants;
import com.xy.meet.entity.Message;
import com.xy.meet.entity.Room;
import com.xy.meet.entity.User;
import com.xy.spring.annotations.core.Component;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@ChannelHandler.Sharable
public class IMeetChatServerHandler extends SimpleChannelInboundHandler<Message> {

    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        handleMessage(ctx, msg);
    }

    private void handleMessage(ChannelHandlerContext ctx, Message message) {
        if (message == null || message.getType() == null) {
            sendError(ctx, "invalid message");
            return;
        }

        String type = message.getType();
        String roomId = message.getRoomId();
        String userId = message.getUserId();

        switch (type) {
            case Constants.MSG_JOIN:
                joinRoom(ctx, roomId, userId, message);
                break;
            case Constants.MSG_LEAVE:
                leaveRoom(ctx, roomId, userId);
                break;
            case Constants.MSG_SIGNAL:
                forwardSignal(roomId, message);
                break;
            case Constants.MSG_UPDATE:
                userUpdate(roomId, userId, message);
                break;
            case Constants.MSG_HEARTBEAT:
                sendHeartbeatAck(ctx, roomId, userId);
                break;
            default:
                sendUnknownMessageType(ctx, type);
                break;
        }
    }

    private void userUpdate(String roomId, String userId, Message message) {

        Room room = rooms.get(roomId);

        if (room != null) {

            log.info("房间 {} 用户 {} 更新状态 ", roomId, userId);
            // 将客户端原始 message 的内容注入到广播消息中（保留 body/stream/user 等）
            Message m = createRoomMessage(Constants.MSG_UPDATE, roomId, userId, null, room, message);

            broadcastToRoom(room, m, userId, false);
        }
    }

    private void joinRoom(ChannelHandlerContext ctx, String roomId, String userId, Message message) {
        Room room = rooms.computeIfAbsent(roomId, Room::new);

        User user = new User(userId, ctx.channel());

        user.updateFrom(message.getUser());

        room.addUser(user);

        log.info("用户 {} 加入房间 {}", userId, roomId);

        // 包装并广播：将客户端原始 message 合并到广播消息中（如果原始包含额外字段，会被保留下来）
        Message joinMessage = createRoomMessage(Constants.MSG_JOIN, roomId, userId, null, room, message);
        // 广播加入消息
        broadcastToRoom(room, joinMessage, userId, true);
    }

    private void leaveRoom(ChannelHandlerContext ctx, String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            // 移除用户
            room.removeUserById(userId);
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
     * 更稳健的 createRoomMessage：优先合并 original 中的字段（body/stream/user），
     * 并安全复制 room.users 为副本（避免并发修改问题）。
     */
    private Message createRoomMessage(String type, String roomId, String userId, String body, Room room, Message original) {
        Message message = new Message();

        // 基本字段
        message.setType(type != null ? type : "unknown");
        message.setRoomId(roomId);
        message.setUserId(userId);

        // 安全复制 users（返回副本或空集合）
        Set<User> safeUsers = safeUsersOf(room);
        message.setUsers(safeUsers);

        // 如果 original 存在且包含 user/body/stream，则优先保留
        if (original != null) {
            if (original.getUser() != null) {
                message.setUser(original.getUser());
            }
            if (original.getBody() != null) {
                message.setBody(original.getBody());
            }
            if (original.getStream() != null) {
                message.setStream(original.getStream());
            }
        }

        // 否则使用传入 body 或根据 type 填充默认 body
        if (message.getBody() == null) {
            if (body != null) {
                message.setBody(body);
            } else {
                switch (message.getType()) {
                    case Constants.MSG_JOIN:

                        message.setBody("joined");
                        break;
                    case Constants.MSG_LEAVE:

                        message.setBody("leaved");
                        break;
                    case Constants.MSG_SIGNAL:

                        message.setBody("signal");
                        break;
                    case Constants.MSG_UPDATE:

                        message.setBody("updated");
                        break;
                    case Constants.MSG_HEARTBEAT:
                        message.setBody("heartbeat_ack");
                        break;
                    default:
                        message.setBody("unknown_type");
                        break;
                }
            }
        }

        // 附加：提供一个简洁的 userId 列表（放入 stream 字段，避免将 Channel 等不可序列化对象发去客户端）
        if ("join".equals(message.getType()) || "leave".equals(message.getType()) || "update".equals(message.getType())) {
            LinkedHashSet<String> userIds = safeUsers.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            // 使用 JSON 更好，但为简单起见，此处用逗号分隔
            message.setStream(String.join(",", userIds));
        }

        return message;
    }

    /**
     * 兼容原签名的重载：不传 original
     */
    private Message createRoomMessage(String type, String roomId, String userId, String body, Room room) {
        return createRoomMessage(type, roomId, userId, body, room, null);
    }

    /**
     * 安全复制 room.users 到副本集合（若 null/空则返回空集合）
     */
    private Set<User> safeUsersOf(Room room) {
        if (room == null || room.getUsers() == null || room.getUsers().isEmpty()) {
            return Collections.emptySet();
        }
        // 返回 HashSet 副本，避免外部修改影响 Message.users
        return new HashSet<>(room.getUsers());
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

    private void sendError(ChannelHandlerContext ctx, String body) {
        Message m = new Message();
        m.setBody(body);
        ctx.writeAndFlush(m);
    }
}

