package com.xy.lucky.live.webrtc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 房间与会话管理
 * - 高并发环境下使用并发容器
 * - 简易路由：将SDP/ICE消息透传给同房间目标端
 */
@Component
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<Channel, String> channelIndex = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void joinRoom(String roomId, String userId, Channel ch) {
        Room room = rooms.computeIfAbsent(roomId, id -> new Room(id));
        room.join(userId, ch);
        channelIndex.put(ch, userId + "@" + roomId);
        log.info("用户加入房间: room={}, user={}", roomId, userId);
    }

    public void leaveRoom(String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.leave(userId);
            log.info("用户离开房间: room={}, user={}", roomId, userId);
            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    public void cleanupByChannel(Channel ch) {
        String tag = channelIndex.remove(ch);
        if (tag == null) return;
        String[] parts = tag.split("@", 2);
        if (parts.length != 2) return;
        leaveRoom(parts[1], parts[0]);
    }

    public void forward(String roomId, String fromUser, String toUser, String type, JsonNode payload) {
        Room room = rooms.get(roomId);
        if (room == null) return;
        Channel target = room.getChannel(toUser);
        if (target == null) return;
        try {
            var node = mapper.createObjectNode()
                    .put("type", type)
                    .put("from", fromUser)
                    .put("roomId", roomId)
                    .set("payload", payload);
            target.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(node.toString()));
        } catch (Exception e) {
            log.warn("消息转发异常: room={}, from={}, to={}, type={}", roomId, fromUser, toUser, type, e);
        }
    }

    static final class Room {
        private final String id;
        private final Map<String, Channel> peers = new ConcurrentHashMap<>();
        private final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        Room(String id) {
            this.id = id;
        }

        void join(String userId, Channel ch) {
            peers.put(userId, ch);
            group.add(ch);
        }

        void leave(String userId) {
            Channel ch = peers.remove(userId);
            if (ch != null) {
                group.remove(ch);
            }
        }

        boolean isEmpty() {
            return peers.isEmpty();
        }

        Channel getChannel(String userId) {
            return peers.get(userId);
        }
    }
}

