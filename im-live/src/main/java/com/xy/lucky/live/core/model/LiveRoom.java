package com.xy.lucky.live.core.model;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 直播房间实体
 * <p>
 * 表示一个直播房间，包含房间成员、推流者、订阅关系等信息
 * 使用线程安全的数据结构确保高并发下的正确性
 *
 * @author lucky
 */
@Data
public class LiveRoom {

    /**
     * 房间 ID
     */
    private final String roomId;
    /**
     * 房间创建时间
     */
    private final long createTime;
    /**
     * 最后活跃时间（用于空闲检测）
     */
    private final AtomicLong lastActiveTime;
    /**
     * 房间内所有用户的 Channel 组
     * 用于高效的广播消息
     */
    private final ChannelGroup channels;
    /**
     * 用户 ID -> 用户信息映射
     */
    private final Map<String, LiveUser> users;
    /**
     * 流 ID -> 流信息映射
     */
    private final Map<String, LiveStream> streams;
    /**
     * 房间名称
     */
    private String name;
    /**
     * 房间是否已关闭
     */
    private volatile boolean closed = false;

    /**
     * 构造函数
     *
     * @param roomId 房间 ID
     */
    public LiveRoom(String roomId) {
        this.roomId = roomId;
        this.createTime = System.currentTimeMillis();
        this.lastActiveTime = new AtomicLong(this.createTime);
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.users = new ConcurrentHashMap<>();
        this.streams = new ConcurrentHashMap<>();
    }

    /**
     * 添加用户
     *
     * @param user 用户信息
     * @return 是否添加成功
     */
    public boolean addUser(LiveUser user) {
        if (closed) {
            return false;
        }
        LiveUser existing = users.putIfAbsent(user.getUserId(), user);
        if (existing == null) {
            if (user.getChannel() != null) {
                channels.add(user.getChannel());
            }
            updateActiveTime();
            return true;
        }
        return false;
    }

    /**
     * 移除用户
     *
     * @param userId 用户 ID
     * @return 被移除的用户，若不存在则返回 null
     */
    public LiveUser removeUser(String userId) {
        LiveUser user = users.remove(userId);
        if (user != null) {
            if (user.getChannel() != null) {
                channels.remove(user.getChannel());
            }
            updateActiveTime();
        }
        return user;
    }

    /**
     * 根据 Channel 移除用户
     *
     * @param channel Channel
     * @return 被移除的用户，若不存在则返回 null
     */
    public LiveUser removeUserByChannel(Channel channel) {
        for (Map.Entry<String, LiveUser> entry : users.entrySet()) {
            if (entry.getValue().getChannel().equals(channel)) {
                return removeUser(entry.getKey());
            }
        }
        return null;
    }

    /**
     * 获取用户
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    public LiveUser getUser(String userId) {
        return users.get(userId);
    }

    /**
     * 检查用户是否存在
     *
     * @param userId 用户 ID
     * @return 是否存在
     */
    public boolean hasUser(String userId) {
        return users.containsKey(userId);
    }

    /**
     * 添加流
     *
     * @param stream 流信息
     * @return 是否添加成功
     */
    public boolean addStream(LiveStream stream) {
        if (closed) {
            return false;
        }
        LiveStream existing = streams.putIfAbsent(stream.getStreamId(), stream);
        if (existing == null) {
            updateActiveTime();
            return true;
        }
        return false;
    }

    /**
     * 移除流
     *
     * @param streamId 流 ID
     * @return 被移除的流，若不存在则返回 null
     */
    public LiveStream removeStream(String streamId) {
        LiveStream stream = streams.remove(streamId);
        if (stream != null) {
            updateActiveTime();
        }
        return stream;
    }

    /**
     * 获取流
     *
     * @param streamId 流 ID
     * @return 流信息
     */
    public LiveStream getStream(String streamId) {
        return streams.get(streamId);
    }

    /**
     * 检查流是否存在
     *
     * @param streamId 流 ID
     * @return 是否存在
     */
    public boolean hasStream(String streamId) {
        return streams.containsKey(streamId);
    }

    /**
     * 获取房间内用户数量
     *
     * @return 用户数量
     */
    public int getUserCount() {
        return users.size();
    }

    /**
     * 获取房间内推流数量
     *
     * @return 推流数量
     */
    public int getStreamCount() {
        return streams.size();
    }

    /**
     * 房间是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return users.isEmpty();
    }

    /**
     * 更新活跃时间
     */
    public void updateActiveTime() {
        lastActiveTime.set(System.currentTimeMillis());
    }

    /**
     * 获取空闲时间（毫秒）
     *
     * @return 空闲时间
     */
    public long getIdleTime() {
        return System.currentTimeMillis() - lastActiveTime.get();
    }

    /**
     * 关闭房间
     * 关闭所有连接并清理资源
     */
    public void close() {
        closed = true;
        streams.clear();
        users.clear();
        channels.close();
    }

    /**
     * 获取房间信息快照（用于响应）
     *
     * @return 房间信息 Map
     */
    public Map<String, Object> toSnapshot() {
        return Map.of(
                "roomId", roomId,
                "name", name != null ? name : roomId,
                "userCount", getUserCount(),
                "streamCount", getStreamCount(),
                "createTime", createTime,
                "users", users.keySet(),
                "streams", streams.keySet()
        );
    }
}

