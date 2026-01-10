package com.xy.lucky.live.core;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.model.LiveRoom;
import com.xy.lucky.live.core.model.LiveStream;
import com.xy.lucky.live.core.model.LiveUser;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 流管理器
 * <p>
 * 负责管理直播流的发布、订阅、取消等操作
 * 与 RoomManager 配合完成完整的推拉流逻辑
 *
 * @author lucky
 */
@Component
public class StreamManager {

    private static final Logger log = LoggerFactory.getLogger(StreamManager.class);

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private LiveProperties liveProperties;

    /**
     * 发布流
     * <p>
     * 如果房间不存在，会自动创建房间。
     * 如果用户不存在，会自动创建用户并加入房间。
     *
     * @param roomId     房间 ID
     * @param userId     用户 ID
     * @param streamId   流 ID
     * @param streamName 流名称
     * @param streamType 流类型
     * @param hasAudio   是否有音频
     * @param hasVideo   是否有视频
     * @return 发布结果
     */
    public PublishResult publish(String roomId, String userId, String streamId,
                                 String streamName, LiveStream.StreamType streamType,
                                 boolean hasAudio, boolean hasVideo) {

        // 自动创建房间（如果不存在）
        LiveRoom room = roomManager.getOrCreateRoom(roomId);

        // 检查用户是否在房间中，如果不存在则自动创建并加入
        LiveUser user = room.getUser(userId);
        if (user == null) {
            // 自动创建用户并加入房间（HTTP API 场景下 Channel 为 null）
            user = LiveUser.builder()
                    .userId(userId)
                    .name(userId)  // 默认使用 userId 作为名称
                    .roomId(roomId)
                    .channel(null)  // HTTP API 场景下没有 WebSocket Channel
                    .build();

            // 检查房间是否已满
            if (room.getUserCount() >= liveProperties.getRoom().getMaxUsersPerRoom()) {
                return PublishResult.failed("房间用户数量已达上限");
            }

            // 添加用户到房间
            if (!room.addUser(user)) {
                return PublishResult.failed("添加用户到房间失败");
            }

            log.info("自动创建用户 {} 并加入房间 {}", userId, roomId);
        }

        // 检查推流数量限制
        if (room.getStreamCount() >= liveProperties.getRoom().getMaxPublishersPerRoom()) {
            return PublishResult.failed("房间推流数量已达上限");
        }

        // 检查流 ID 是否已存在
        if (room.hasStream(streamId)) {
            return PublishResult.failed("流 ID 已存在");
        }

        // 创建流
        LiveStream stream = LiveStream.builder()
                .streamId(streamId)
                .name(streamName != null ? streamName : streamId)
                .roomId(roomId)
                .publisherId(userId)
                .type(streamType != null ? streamType : LiveStream.StreamType.CAMERA)
                .hasAudio(hasAudio)
                .hasVideo(hasVideo)
                .build();

        // 添加到房间
        if (room.addStream(stream)) {
            user.addPublishedStream(streamId);
            log.info("用户 {} 在房间 {} 发布流: {}", userId, roomId, streamId);
            return PublishResult.success(stream);
        }

        return PublishResult.failed("发布流失败");
    }

    /**
     * 停止发布流
     *
     * @param roomId   房间 ID
     * @param userId   用户 ID
     * @param streamId 流 ID
     * @return 停止发布结果
     */
    public UnpublishResult unpublish(String roomId, String userId, String streamId) {
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return UnpublishResult.failed("房间不存在");
        }

        LiveStream stream = room.getStream(streamId);
        if (stream == null) {
            return UnpublishResult.failed("流不存在");
        }

        // 检查是否是流的发布者
        if (!stream.getPublisherId().equals(userId)) {
            return UnpublishResult.failed("无权停止该流");
        }

        // 获取订阅者列表（用于通知）
        Set<String> subscribers = new HashSet<>(stream.getSubscribers());

        // 从房间移除流
        room.removeStream(streamId);

        // 更新用户状态
        LiveUser user = room.getUser(userId);
        if (user != null) {
            user.removePublishedStream(streamId);
        }

        // 更新订阅者状态
        for (String subscriberId : subscribers) {
            LiveUser subscriber = room.getUser(subscriberId);
            if (subscriber != null) {
                subscriber.removeSubscribedStream(streamId);
            }
        }

        log.info("用户 {} 在房间 {} 停止发布流: {}，影响订阅者: {}", userId, roomId, streamId, subscribers.size());
        return UnpublishResult.success(stream, subscribers);
    }

    /**
     * 订阅流
     *
     * @param roomId   房间 ID
     * @param userId   用户 ID
     * @param streamId 流 ID
     * @return 订阅结果
     */
    public SubscribeResult subscribe(String roomId, String userId, String streamId) {
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return SubscribeResult.failed("房间不存在");
        }

        LiveUser user = room.getUser(userId);
        if (user == null) {
            return SubscribeResult.failed("用户不在房间中");
        }

        LiveStream stream = room.getStream(streamId);
        if (stream == null) {
            return SubscribeResult.failed("流不存在");
        }

        // 不能订阅自己发布的流
        if (stream.getPublisherId().equals(userId)) {
            return SubscribeResult.failed("不能订阅自己的流");
        }

        // 添加订阅关系
        stream.addSubscriber(userId);
        user.addSubscribedStream(streamId);

        log.info("用户 {} 在房间 {} 订阅流: {}", userId, roomId, streamId);
        return SubscribeResult.success(stream);
    }

    /**
     * 取消订阅流
     *
     * @param roomId   房间 ID
     * @param userId   用户 ID
     * @param streamId 流 ID
     * @return 取消订阅结果
     */
    public UnsubscribeResult unsubscribe(String roomId, String userId, String streamId) {
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return UnsubscribeResult.failed("房间不存在");
        }

        LiveUser user = room.getUser(userId);
        if (user == null) {
            return UnsubscribeResult.failed("用户不在房间中");
        }

        LiveStream stream = room.getStream(streamId);
        if (stream == null) {
            // 流可能已被停止，只更新用户状态
            user.removeSubscribedStream(streamId);
            return UnsubscribeResult.success(null);
        }

        stream.removeSubscriber(userId);
        user.removeSubscribedStream(streamId);

        log.info("用户 {} 在房间 {} 取消订阅流: {}", userId, roomId, streamId);
        return UnsubscribeResult.success(stream);
    }

    /**
     * 获取房间内所有流
     *
     * @param roomId 房间 ID
     * @return 流列表
     */
    public List<LiveStream> getRoomStreams(String roomId) {
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(room.getStreams().values());
    }

    /**
     * 获取流信息
     *
     * @param roomId   房间 ID
     * @param streamId 流 ID
     * @return 流信息
     */
    public LiveStream getStream(String roomId, String streamId) {
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return null;
        }
        return room.getStream(streamId);
    }

    /**
     * 获取用户发布的所有流
     *
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @return 流列表
     */
    public List<LiveStream> getUserPublishedStreams(String roomId, String userId) {
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return Collections.emptyList();
        }

        LiveUser user = room.getUser(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        List<LiveStream> streams = new ArrayList<>();
        for (String streamId : user.getPublishedStreams()) {
            LiveStream stream = room.getStream(streamId);
            if (stream != null) {
                streams.add(stream);
            }
        }
        return streams;
    }

    // ==================== 结果类 ====================

    /**
     * 发布结果
     */
    public static class PublishResult {
        private final boolean success;
        private final LiveStream stream;
        private final String error;

        private PublishResult(boolean success, LiveStream stream, String error) {
            this.success = success;
            this.stream = stream;
            this.error = error;
        }

        public static PublishResult success(LiveStream stream) {
            return new PublishResult(true, stream, null);
        }

        public static PublishResult failed(String error) {
            return new PublishResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public LiveStream getStream() {
            return stream;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * 停止发布结果
     */
    public static class UnpublishResult {
        private final boolean success;
        private final LiveStream stream;
        private final Set<String> affectedSubscribers;
        private final String error;

        private UnpublishResult(boolean success, LiveStream stream, Set<String> affectedSubscribers, String error) {
            this.success = success;
            this.stream = stream;
            this.affectedSubscribers = affectedSubscribers;
            this.error = error;
        }

        public static UnpublishResult success(LiveStream stream, Set<String> subscribers) {
            return new UnpublishResult(true, stream, subscribers, null);
        }

        public static UnpublishResult failed(String error) {
            return new UnpublishResult(false, null, Collections.emptySet(), error);
        }

        public boolean isSuccess() {
            return success;
        }

        public LiveStream getStream() {
            return stream;
        }

        public Set<String> getAffectedSubscribers() {
            return affectedSubscribers;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * 订阅结果
     */
    public static class SubscribeResult {
        private final boolean success;
        private final LiveStream stream;
        private final String error;

        private SubscribeResult(boolean success, LiveStream stream, String error) {
            this.success = success;
            this.stream = stream;
            this.error = error;
        }

        public static SubscribeResult success(LiveStream stream) {
            return new SubscribeResult(true, stream, null);
        }

        public static SubscribeResult failed(String error) {
            return new SubscribeResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public LiveStream getStream() {
            return stream;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * 取消订阅结果
     */
    public static class UnsubscribeResult {
        private final boolean success;
        private final LiveStream stream;
        private final String error;

        private UnsubscribeResult(boolean success, LiveStream stream, String error) {
            this.success = success;
            this.stream = stream;
            this.error = error;
        }

        public static UnsubscribeResult success(LiveStream stream) {
            return new UnsubscribeResult(true, stream, null);
        }

        public static UnsubscribeResult failed(String error) {
            return new UnsubscribeResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public LiveStream getStream() {
            return stream;
        }

        public String getError() {
            return error;
        }
    }
}

