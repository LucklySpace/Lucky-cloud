package com.xy.lucky.live.core;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.model.LiveRoom;
import com.xy.lucky.live.core.model.LiveUser;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import com.xy.lucky.spring.annotations.core.PreDestroy;
import com.xy.lucky.spring.core.DisposableBean;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 房间管理器
 * <p>
 * 负责管理所有直播房间的生命周期，包括创建、查询、销毁等操作
 * 使用 ConcurrentHashMap 保证线程安全，使用虚拟线程处理清理任务
 *
 * @author lucky
 */
@Component
public class RoomManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RoomManager.class);

    /**
     * 房间 ID -> 房间实例映射
     */
    private final Map<String, LiveRoom> rooms = new ConcurrentHashMap<>();

    /**
     * Channel -> 用户信息映射（用于快速查找断线用户）
     */
    private final Map<Channel, UserLocation> channelUserMap = new ConcurrentHashMap<>();

    /**
     * 定时清理调度器
     */
    private ScheduledExecutorService cleanupScheduler;

    @Autowired
    private LiveProperties liveProperties;

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        // 使用虚拟线程执行器进行定时清理
        cleanupScheduler = Executors.newScheduledThreadPool(1, Thread.ofVirtual()
                .name("room-cleanup-", 0)
                .factory());

        // 每分钟执行一次空闲房间清理
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupIdleRooms,
                60, 60, TimeUnit.SECONDS
        );

        log.info("RoomManager 初始化完成，空闲超时: {}ms", liveProperties.getRoom().getIdleTimeout());
    }

    /**
     * 获取或创建房间
     *
     * @param roomId 房间 ID
     * @return 房间实例
     */
    public LiveRoom getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> {
            LiveRoom room = new LiveRoom(id);
            log.info("创建房间: {}", id);
            return room;
        });
    }

    /**
     * 获取房间
     *
     * @param roomId 房间 ID
     * @return 房间实例，不存在则返回 null
     */
    public LiveRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public LiveUser getUser(String roomId, String userId) {
        LiveRoom room = rooms.get(roomId);
        if (room == null) {
            return null;
        }
        return room.getUser(userId);
    }


    /**
     * 检查房间是否存在
     *
     * @param roomId 房间 ID
     * @return 是否存在
     */
    public boolean hasRoom(String roomId) {
        return rooms.containsKey(roomId);
    }

    /**
     * 用户加入房间
     *
     * @param roomId  房间 ID
     * @param userId  用户 ID
     * @param channel 用户 Channel
     * @param name    用户名称
     * @return 加入结果
     */
    public JoinResult joinRoom(String roomId, String userId, Channel channel, String name) {
        LiveRoom room = getOrCreateRoom(roomId);

        // 检查房间是否已满
        if (room.getUserCount() >= liveProperties.getRoom().getMaxUsersPerRoom()) {
            return JoinResult.roomFull();
        }

        // 检查用户是否已在房间中
        if (room.hasUser(userId)) {
            return JoinResult.alreadyJoined();
        }

        // 创建用户并加入房间
        LiveUser user = LiveUser.builder()
                .userId(userId)
                .name(name)
                .roomId(roomId)
                .channel(channel)
                .build();

        if (room.addUser(user)) {
            // 记录 Channel -> 用户位置映射
            channelUserMap.put(channel, new UserLocation(roomId, userId));
            log.info("用户 {} 加入房间 {}，当前人数: {}", userId, roomId, room.getUserCount());
            return JoinResult.success(room, user);
        }

        return JoinResult.failed("加入房间失败");
    }

    /**
     * 用户离开房间
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @return 离开的用户，若不存在则返回 null
     */
    public LiveUser leaveRoom(String roomId, String userId) {
        LiveRoom room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        LiveUser user = room.removeUser(userId);
        if (user != null) {
            channelUserMap.remove(user.getChannel());
            log.info("用户 {} 离开房间 {}，当前人数: {}", userId, roomId, room.getUserCount());

            // 清理用户发布的流
            for (String streamId : user.getPublishedStreams()) {
                room.removeStream(streamId);
            }

            // 如果房间为空，标记待清理
            if (room.isEmpty()) {
                log.info("房间 {} 已空，等待清理", roomId);
            }
        }
        return user;
    }

    /**
     * 根据 Channel 处理用户断线
     *
     * @param channel 断线的 Channel
     * @return 断线的用户信息
     */
    public LiveUser handleDisconnect(Channel channel) {
        UserLocation location = channelUserMap.remove(channel);
        if (location == null) {
            return null;
        }
        return leaveRoom(location.roomId, location.userId);
    }

    /**
     * 根据 Channel 获取用户位置
     *
     * @param channel Channel
     * @return 用户位置
     */
    public UserLocation getUserLocation(Channel channel) {
        return channelUserMap.get(channel);
    }

    /**
     * 获取所有房间 ID
     *
     * @return 房间 ID 集合
     */
    public Set<String> getAllRoomIds() {
        return Collections.unmodifiableSet(rooms.keySet());
    }

    /**
     * 获取房间数量
     *
     * @return 房间数量
     */
    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * 获取总用户数
     *
     * @return 总用户数
     */
    public int getTotalUserCount() {
        return rooms.values().stream()
                .mapToInt(LiveRoom::getUserCount)
                .sum();
    }

    /**
     * 销毁房间
     *
     * @param roomId 房间 ID
     * @return 是否销毁成功
     */
    public boolean destroyRoom(String roomId) {
        LiveRoom room = rooms.remove(roomId);
        if (room != null) {
            // 清理 Channel 映射
            for (LiveUser user : room.getUsers().values()) {
                channelUserMap.remove(user.getChannel());
            }
            room.close();
            log.info("房间 {} 已销毁", roomId);
            return true;
        }
        return false;
    }

    /**
     * 清理空闲房间
     */
    private void cleanupIdleRooms() {
        long idleTimeout = liveProperties.getRoom().getIdleTimeout();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, LiveRoom> entry : rooms.entrySet()) {
            LiveRoom room = entry.getValue();
            if (room.isEmpty() && room.getIdleTime() > idleTimeout) {
                toRemove.add(entry.getKey());
            }
        }

        for (String roomId : toRemove) {
            destroyRoom(roomId);
            log.info("清理空闲房间: {}", roomId);
        }
    }

    /**
     * 销毁时清理资源
     */
    @Override
    @PreDestroy
    public void destroy() {
        log.info("RoomManager 正在关闭...");

        // 停止清理任务
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
            try {
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭所有房间
        for (String roomId : new ArrayList<>(rooms.keySet())) {
            destroyRoom(roomId);
        }

        channelUserMap.clear();
        log.info("RoomManager 已关闭");
    }

    /**
     * 用户位置信息
     */
    public record UserLocation(String roomId, String userId) {
    }

    /**
     * 加入房间结果
     */
    public static class JoinResult {
        private final boolean success;
        private final LiveRoom room;
        private final LiveUser user;
        private final String error;

        private JoinResult(boolean success, LiveRoom room, LiveUser user, String error) {
            this.success = success;
            this.room = room;
            this.user = user;
            this.error = error;
        }

        public static JoinResult success(LiveRoom room, LiveUser user) {
            return new JoinResult(true, room, user, null);
        }

        public static JoinResult roomFull() {
            return new JoinResult(false, null, null, "房间已满");
        }

        public static JoinResult alreadyJoined() {
            return new JoinResult(false, null, null, "已在房间中");
        }

        public static JoinResult failed(String error) {
            return new JoinResult(false, null, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public LiveRoom getRoom() {
            return room;
        }

        public LiveUser getUser() {
            return user;
        }

        public String getError() {
            return error;
        }
    }
}

