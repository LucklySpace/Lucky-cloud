package com.xy.lucky.live.signal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.RoomManager;
import com.xy.lucky.live.core.StreamManager;
import com.xy.lucky.live.core.model.LiveRoom;
import com.xy.lucky.live.core.model.LiveStream;
import com.xy.lucky.live.core.model.LiveUser;
import com.xy.lucky.live.core.sfu.*;
import com.xy.lucky.live.signal.message.ErrorCode;
import com.xy.lucky.live.signal.message.MessageType;
import com.xy.lucky.live.signal.message.SignalMessage;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.PostConstruct;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 信令分发器
 * <p>
 * 负责解析和分发 WebRTC 信令消息，协调房间管理和流管理
 * 使用虚拟线程执行业务逻辑，避免阻塞 Netty 的 EventLoop
 *
 * @author lucky
 */
@Component
public class SignalDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SignalDispatcher.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 虚拟线程执行器，用于处理业务逻辑
     */
    private ExecutorService virtualExecutor;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private StreamManager streamManager;

    @Autowired
    private LiveProperties liveProperties;

    @Autowired
    private MediaForwarder mediaForwarder;

    @Autowired
    private ConnectionManager connectionManager;

    @Autowired
    private QosMonitor qosMonitor;

    @PostConstruct
    public void init() {
        // 创建虚拟线程执行器
        if (liveProperties.getPerformance().isUseVirtualThreads()) {
            virtualExecutor = Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name(liveProperties.getPerformance().getVirtualThreadNamePrefix(), 0)
                            .factory()
            );
            log.info("SignalDispatcher 使用虚拟线程执行器");
        } else {
            virtualExecutor = Executors.newCachedThreadPool();
            log.info("SignalDispatcher 使用普通线程池");
        }
    }

    /**
     * 处理接收到的 WebSocket 消息
     *
     * @param ctx     Channel 上下文
     * @param message 消息内容
     */
    public void dispatch(ChannelHandlerContext ctx, String message) {
        // 记录请求开始时间
        final long startTime = System.currentTimeMillis();

        // 在虚拟线程中处理业务逻辑
        virtualExecutor.execute(() -> {
            try {
                SignalMessage msg = objectMapper.readValue(message, SignalMessage.class);
                handleMessage(ctx, msg);

                // 记录延迟和请求
                qosMonitor.recordLatency(System.currentTimeMillis() - startTime);
                qosMonitor.recordRequest();
            } catch (JsonProcessingException e) {
                log.warn("消息解析失败: {}", message, e);
                sendError(ctx.channel(), MessageType.ERROR, ErrorCode.INVALID_MESSAGE, "消息格式错误");
                qosMonitor.recordError("INVALID_MESSAGE");
            } catch (Exception e) {
                log.error("处理消息异常: {}", message, e);
                sendError(ctx.channel(), MessageType.ERROR, ErrorCode.INTERNAL_ERROR, "服务器内部错误");
                qosMonitor.recordError("INTERNAL_ERROR");
            }
        });
    }

    /**
     * 处理用户断开连接
     *
     * @param ctx Channel 上下文
     */
    public void handleDisconnect(ChannelHandlerContext ctx) {
        virtualExecutor.execute(() -> {
            try {
                // 记录连接关闭
                qosMonitor.recordConnectionClose();

                // 清理 SFU 相关资源
                List<String> affectedStreams = mediaForwarder.handleChannelClosed(ctx.channel());

                LiveUser user = roomManager.handleDisconnect(ctx.channel());
                if (user != null) {
                    // 清理用户相关的所有连接
                    connectionManager.removeConnectionsByUser(user.getUserId());

                    // 销毁用户发布的所有流的转发会话
                    for (String streamId : user.getPublishedStreams()) {
                        mediaForwarder.destroySession(streamId);
                        connectionManager.removeConnectionsByStream(streamId);
                    }

                    // 广播用户离开通知
                    LiveRoom room = roomManager.getRoom(user.getRoomId());
                    if (room != null) {
                        broadcastToRoom(room, SignalMessage.builder()
                                .type(MessageType.USER_LEFT)
                                .roomId(user.getRoomId())
                                .userId(user.getUserId())
                                .data(Map.of("userName", user.getName() != null ? user.getName() : user.getUserId()))
                                .timestamp(System.currentTimeMillis())
                                .build(), null);

                        // 广播用户发布的流已停止
                        for (String streamId : user.getPublishedStreams()) {
                            broadcastToRoom(room, SignalMessage.builder()
                                    .type(MessageType.STREAM_UNPUBLISHED)
                                    .roomId(user.getRoomId())
                                    .streamId(streamId)
                                    .userId(user.getUserId())
                                    .timestamp(System.currentTimeMillis())
                                    .build(), null);
                        }
                    }
                    log.info("用户 {} 断开连接并离开房间 {}，清理 {} 个流",
                            user.getUserId(), user.getRoomId(), user.getPublishedStreams().size());
                }
            } catch (Exception e) {
                log.error("处理断开连接异常", e);
                qosMonitor.recordError("DISCONNECT_ERROR");
            }
        });
    }

    /**
     * 根据消息类型分发处理
     */
    private void handleMessage(ChannelHandlerContext ctx, SignalMessage msg) {
        String type = msg.getType();
        if (type == null) {
            sendError(ctx.channel(), MessageType.ERROR, ErrorCode.INVALID_PARAM, "缺少消息类型");
            return;
        }

        switch (type) {
            case MessageType.HEARTBEAT -> handleHeartbeat(ctx, msg);
            case MessageType.JOIN -> handleJoin(ctx, msg);
            case MessageType.LEAVE -> handleLeave(ctx, msg);
            case MessageType.ROOM_INFO -> handleRoomInfo(ctx, msg);
            case MessageType.PUBLISH -> handlePublish(ctx, msg);
            case MessageType.UNPUBLISH -> handleUnpublish(ctx, msg);
            case MessageType.SUBSCRIBE -> handleSubscribe(ctx, msg);
            case MessageType.UNSUBSCRIBE -> handleUnsubscribe(ctx, msg);
            case MessageType.OFFER -> handleOffer(ctx, msg);
            case MessageType.ANSWER -> handleAnswer(ctx, msg);
            case MessageType.CANDIDATE -> handleCandidate(ctx, msg);
            case MessageType.MUTE, MessageType.VIDEO -> handleMediaState(ctx, msg);
            case MessageType.CONNECTION_STATE -> handleConnectionState(ctx, msg);
            case MessageType.STATS -> handleStatsRequest(ctx, msg);
            default -> sendError(ctx.channel(), MessageType.ERROR, ErrorCode.INVALID_PARAM, "未知消息类型: " + type);
        }
    }

    /**
     * 处理连接状态更新
     * <p>
     * 客户端报告 WebRTC 连接状态变化
     */
    private void handleConnectionState(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();
        String streamId = msg.getStreamId();

        if (roomId == null || userId == null || streamId == null || msg.getData() == null) {
            return;
        }

        if (msg.getData() instanceof Map<?, ?> data) {
            String state = (String) data.get("state");
            String iceState = (String) data.get("iceState");

            // 更新 SFU 会话状态
            LiveRoom room = roomManager.getRoom(roomId);
            if (room == null) return;

            LiveStream stream = room.getStream(streamId);
            if (stream == null) return;

            // 判断是推流者还是订阅者
            boolean isPublisher = stream.getPublisherId().equals(userId);
            String publisherId = stream.getPublisherId();
            String subscriberId = isPublisher ? null : userId;

            if (state != null) {
                mediaForwarder.updateConnectionState(streamId, userId, state, isPublisher);

                // 如果是订阅者，更新连接状态
                if (!isPublisher && subscriberId != null) {
                    String connectionId = ConnectionManager.generateConnectionId(publisherId, subscriberId, streamId);
                    connectionManager.updateConnectionState(connectionId, state);
                    if (iceState != null) {
                        connectionManager.updateIceState(connectionId, iceState);
                    }
                }
            }

            log.debug("连接状态更新: streamId={}, userId={}, state={}, iceState={}",
                    streamId, userId, state, iceState);
        }
    }

    /**
     * 处理统计请求
     * <p>
     * 返回服务器的 QoS 统计信息
     */
    private void handleStatsRequest(ChannelHandlerContext ctx, SignalMessage msg) {
        Map<String, Object> stats = qosMonitor.getStats();
        Map<String, Object> forwarderStats = mediaForwarder.getStats();
        Map<String, Object> connectionStats = connectionManager.getStats();

        send(ctx.channel(), SignalMessage.builder()
                .type(MessageType.STATS)
                .code(ErrorCode.SUCCESS)
                .data(Map.of(
                        "qos", stats,
                        "forwarder", forwarderStats,
                        "connections", connectionStats
                ))
                .timestamp(System.currentTimeMillis())
                .build());
    }

    // ==================== 消息处理方法 ====================

    /**
     * 处理心跳
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, SignalMessage msg) {
        send(ctx.channel(), SignalMessage.builder()
                .type(MessageType.PONG)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * 处理加入房间
     */
    private void handleJoin(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();

        if (roomId == null || roomId.isBlank()) {
            sendError(ctx.channel(), MessageType.JOINED, ErrorCode.INVALID_PARAM, "缺少房间ID");
            return;
        }
        if (userId == null || userId.isBlank()) {
            sendError(ctx.channel(), MessageType.JOINED, ErrorCode.INVALID_PARAM, "缺少用户ID");
            return;
        }

        // 获取用户名称
        String userName = userId;
        if (msg.getData() instanceof Map<?, ?> data) {
            Object name = data.get("name");
            if (name != null) {
                userName = name.toString();
            }
        }

        // 加入房间
        RoomManager.JoinResult result = roomManager.joinRoom(roomId, userId, ctx.channel(), userName);

        if (result.isSuccess()) {
            LiveRoom room = result.getRoom();

            // 记录 QoS
            qosMonitor.recordJoin();
            qosMonitor.recordConnectionOpen();

            // 获取房间内已存在的流列表
            List<Map<String, Object>> existingStreams = room.getStreams().values().stream()
                    .map(LiveStream::toSnapshot)
                    .toList();

            // 发送加入成功响应
            send(ctx.channel(), SignalMessage.builder()
                    .type(MessageType.JOINED)
                    .roomId(roomId)
                    .userId(userId)
                    .code(ErrorCode.SUCCESS)
                    .message("加入房间成功")
                    .data(Map.of(
                            "room", room.toSnapshot(),
                            "iceServers", buildIceServers(),
                            "streams", existingStreams  // 返回房间内已有的流
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 广播新用户加入通知
            broadcastToRoom(room, SignalMessage.builder()
                    .type(MessageType.USER_JOINED)
                    .roomId(roomId)
                    .userId(userId)
                    .data(Map.of("userName", userName))
                    .timestamp(System.currentTimeMillis())
                    .build(), userId);

            log.info("用户 {} 加入房间 {}，当前用户数: {}，流数: {}",
                    userId, roomId, room.getUserCount(), room.getStreamCount());
        } else {
            int errorCode = switch (result.getError()) {
                case "房间已满" -> ErrorCode.ROOM_FULL;
                case "已在房间中" -> ErrorCode.ALREADY_IN_ROOM;
                default -> ErrorCode.UNKNOWN_ERROR;
            };
            sendError(ctx.channel(), MessageType.JOINED, errorCode, result.getError());
        }
    }

    /**
     * 处理离开房间
     */
    private void handleLeave(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();

        if (roomId == null || userId == null) {
            sendError(ctx.channel(), MessageType.LEFT, ErrorCode.INVALID_PARAM, "缺少参数");
            return;
        }

        LiveUser user = roomManager.leaveRoom(roomId, userId);
        if (user != null) {
            // 发送离开成功响应
            send(ctx.channel(), SignalMessage.success(MessageType.LEFT, "离开房间成功"));

            // 广播用户离开通知
            LiveRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                broadcastToRoom(room, SignalMessage.builder()
                        .type(MessageType.USER_LEFT)
                        .roomId(roomId)
                        .userId(userId)
                        .data(Map.of("userName", user.getName() != null ? user.getName() : userId))
                        .timestamp(System.currentTimeMillis())
                        .build(), null);

                // 广播用户发布的流已停止
                for (String streamId : user.getPublishedStreams()) {
                    broadcastToRoom(room, SignalMessage.builder()
                            .type(MessageType.STREAM_UNPUBLISHED)
                            .roomId(roomId)
                            .streamId(streamId)
                            .userId(userId)
                            .timestamp(System.currentTimeMillis())
                            .build(), null);
                }
            }
        } else {
            sendError(ctx.channel(), MessageType.LEFT, ErrorCode.NOT_IN_ROOM, "不在房间中");
        }
    }

    /**
     * 处理获取房间信息
     */
    private void handleRoomInfo(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null) {
            sendError(ctx.channel(), MessageType.ROOM_INFO, ErrorCode.INVALID_PARAM, "缺少房间ID");
            return;
        }

        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            sendError(ctx.channel(), MessageType.ROOM_INFO, ErrorCode.ROOM_NOT_FOUND, "房间不存在");
            return;
        }

        // 获取流列表
        List<Map<String, Object>> streams = room.getStreams().values().stream()
                .map(LiveStream::toSnapshot)
                .toList();

        send(ctx.channel(), SignalMessage.builder()
                .type(MessageType.ROOM_INFO)
                .roomId(roomId)
                .code(ErrorCode.SUCCESS)
                .data(Map.of(
                        "room", room.toSnapshot(),
                        "streams", streams
                ))
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * 处理发布流
     */
    private void handlePublish(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();
        String streamId = msg.getStreamId();

        if (roomId == null || userId == null || streamId == null) {
            sendError(ctx.channel(), MessageType.PUBLISHED, ErrorCode.INVALID_PARAM, "缺少参数");
            return;
        }

        // 解析流信息
        String streamName = streamId;
        LiveStream.StreamType streamType = LiveStream.StreamType.CAMERA;
        boolean hasAudio = true;
        boolean hasVideo = true;

        if (msg.getData() instanceof Map<?, ?> data) {
            if (data.get("name") != null) {
                streamName = data.get("name").toString();
            }
            if (data.get("type") != null) {
                try {
                    streamType = LiveStream.StreamType.valueOf(data.get("type").toString().toUpperCase());
                } catch (Exception ignored) {
                }
            }
            if (data.get("hasAudio") != null) {
                hasAudio = Boolean.parseBoolean(data.get("hasAudio").toString());
            }
            if (data.get("hasVideo") != null) {
                hasVideo = Boolean.parseBoolean(data.get("hasVideo").toString());
            }
        }

        StreamManager.PublishResult result = streamManager.publish(
                roomId, userId, streamId, streamName, streamType, hasAudio, hasVideo);

        if (result.isSuccess()) {
            LiveStream stream = result.getStream();

            // 记录 QoS
            qosMonitor.recordPublish();

            // 创建 SFU 转发会话
            ForwardingSession session = mediaForwarder.createSession(roomId, userId, streamId, stream);
            session.setPublisherChannel(ctx.channel());

            // 发送发布成功响应
            send(ctx.channel(), SignalMessage.builder()
                    .type(MessageType.PUBLISHED)
                    .roomId(roomId)
                    .userId(userId)
                    .streamId(streamId)
                    .code(ErrorCode.SUCCESS)
                    .message("发布成功")
                    .data(stream.toSnapshot())
                    .timestamp(System.currentTimeMillis())
                    .build());

            // 广播新流发布通知给房间内其他用户
            LiveRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                broadcastToRoom(room, SignalMessage.builder()
                        .type(MessageType.STREAM_PUBLISHED)
                        .roomId(roomId)
                        .userId(userId)
                        .streamId(streamId)
                        .data(stream.toSnapshot())
                        .timestamp(System.currentTimeMillis())
                        .build(), userId);
            }

            log.info("用户 {} 在房间 {} 发布流 {}", userId, roomId, streamId);
        } else {
            int errorCode = switch (result.getError()) {
                case "流 ID 已存在" -> ErrorCode.STREAM_EXISTS;
                case "房间推流数量已达上限" -> ErrorCode.PUBLISH_LIMIT_EXCEEDED;
                default -> ErrorCode.UNKNOWN_ERROR;
            };
            sendError(ctx.channel(), MessageType.PUBLISHED, errorCode, result.getError());
            qosMonitor.recordError("PUBLISH_FAILED");
        }
    }

    /**
     * 处理停止发布流
     */
    private void handleUnpublish(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();
        String streamId = msg.getStreamId();

        if (roomId == null || userId == null || streamId == null) {
            sendError(ctx.channel(), MessageType.UNPUBLISHED, ErrorCode.INVALID_PARAM, "缺少参数");
            return;
        }

        StreamManager.UnpublishResult result = streamManager.unpublish(roomId, userId, streamId);

        if (result.isSuccess()) {
            // 销毁 SFU 转发会话
            mediaForwarder.destroySession(streamId);

            // 清理相关连接
            connectionManager.removeConnectionsByStream(streamId);

            // 发送停止发布成功响应
            send(ctx.channel(), SignalMessage.success(MessageType.UNPUBLISHED, "停止发布成功"));

            // 广播流停止通知给所有用户（包括订阅者）
            LiveRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                broadcastToRoom(room, SignalMessage.builder()
                        .type(MessageType.STREAM_UNPUBLISHED)
                        .roomId(roomId)
                        .userId(userId)
                        .streamId(streamId)
                        .timestamp(System.currentTimeMillis())
                        .build(), null);
            }

            log.info("用户 {} 在房间 {} 停止发布流 {}", userId, roomId, streamId);
        } else {
            sendError(ctx.channel(), MessageType.UNPUBLISHED, ErrorCode.STREAM_NOT_FOUND, result.getError());
        }
    }

    /**
     * 处理订阅流
     * <p>
     * SFU 模式下的订阅流程：
     * 1. 订阅者发送 subscribe 请求
     * 2. 服务器记录订阅关系，创建连接
     * 3. 返回订阅成功，包含推流者信息
     * 4. 订阅者创建 PeerConnection 并发送 Offer 给推流者
     * 5. 推流者收到 Offer，回复 Answer
     * 6. 双方交换 ICE Candidate 完成连接
     */
    private void handleSubscribe(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();
        String streamId = msg.getStreamId();

        if (roomId == null || userId == null || streamId == null) {
            sendError(ctx.channel(), MessageType.SUBSCRIBED, ErrorCode.INVALID_PARAM, "缺少参数");
            return;
        }

        StreamManager.SubscribeResult result = streamManager.subscribe(roomId, userId, streamId);

        if (result.isSuccess()) {
            LiveStream stream = result.getStream();

            // 记录 QoS
            qosMonitor.recordSubscribe();

            // 添加到 SFU 转发会话
            mediaForwarder.addSubscriber(streamId, userId, ctx.channel());

            // 获取推流者信息
            LiveRoom room = roomManager.getRoom(roomId);
            LiveUser publisher = room != null ? room.getUser(stream.getPublisherId()) : null;

            // 创建连接记录
            if (publisher != null) {
                connectionManager.createConnection(
                        stream.getPublisherId(), userId, streamId,
                        publisher.getChannel(), ctx.channel()
                );
            }

            // 发送订阅成功响应，包含推流者信息
            send(ctx.channel(), SignalMessage.builder()
                    .type(MessageType.SUBSCRIBED)
                    .roomId(roomId)
                    .userId(userId)
                    .streamId(streamId)
                    .code(ErrorCode.SUCCESS)
                    .message("订阅成功")
                    .data(Map.of(
                            "stream", stream.toSnapshot(),
                            "publisherId", stream.getPublisherId()
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build());

            log.info("用户 {} 在房间 {} 订阅流 {} (推流者: {})",
                    userId, roomId, streamId, stream.getPublisherId());
        } else {
            sendError(ctx.channel(), MessageType.SUBSCRIBED, ErrorCode.STREAM_NOT_FOUND, result.getError());
            qosMonitor.recordError("SUBSCRIBE_FAILED");
        }
    }

    /**
     * 处理取消订阅流
     */
    private void handleUnsubscribe(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();
        String streamId = msg.getStreamId();

        if (roomId == null || userId == null || streamId == null) {
            sendError(ctx.channel(), MessageType.UNSUBSCRIBED, ErrorCode.INVALID_PARAM, "缺少参数");
            return;
        }

        StreamManager.UnsubscribeResult result = streamManager.unsubscribe(roomId, userId, streamId);

        if (result.isSuccess()) {
            // 从 SFU 转发会话移除订阅者
            mediaForwarder.removeSubscriber(streamId, userId);

            // 获取推流者 ID 并移除连接
            LiveStream stream = result.getStream();
            if (stream != null) {
                String connectionId = ConnectionManager.generateConnectionId(
                        stream.getPublisherId(), userId, streamId);
                connectionManager.removeConnection(connectionId);
            }

            send(ctx.channel(), SignalMessage.success(MessageType.UNSUBSCRIBED, "取消订阅成功"));
            log.info("用户 {} 在房间 {} 取消订阅流 {}", userId, roomId, streamId);
        } else {
            sendError(ctx.channel(), MessageType.UNSUBSCRIBED, ErrorCode.UNKNOWN_ERROR, result.getError());
        }
    }

    /**
     * 处理 SDP Offer
     */
    private void handleOffer(ChannelHandlerContext ctx, SignalMessage msg) {
        forwardSignal(ctx, msg, MessageType.OFFER);
    }

    /**
     * 处理 SDP Answer
     */
    private void handleAnswer(ChannelHandlerContext ctx, SignalMessage msg) {
        forwardSignal(ctx, msg, MessageType.ANSWER);
    }

    /**
     * 处理 ICE Candidate
     */
    private void handleCandidate(ChannelHandlerContext ctx, SignalMessage msg) {
        forwardSignal(ctx, msg, MessageType.CANDIDATE);
    }

    /**
     * 转发信令消息到目标用户
     * <p>
     * 在 SFU 模式下，信令转发同时会更新连接状态
     */
    private void forwardSignal(ChannelHandlerContext ctx, SignalMessage msg, String type) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();
        String targetId = msg.getTargetId();
        String streamId = msg.getStreamId();

        if (roomId == null || userId == null || targetId == null) {
            sendError(ctx.channel(), type, ErrorCode.INVALID_PARAM, "缺少参数");
            return;
        }

        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            sendError(ctx.channel(), type, ErrorCode.ROOM_NOT_FOUND, "房间不存在");
            return;
        }

        LiveUser targetUser = room.getUser(targetId);
        if (targetUser == null) {
            sendError(ctx.channel(), type, ErrorCode.TARGET_UNREACHABLE, "目标用户不在房间中");
            return;
        }

        // 更新连接状态
        if (streamId != null) {
            // 确定推流者和订阅者
            LiveStream stream = room.getStream(streamId);
            if (stream != null) {
                String publisherId = stream.getPublisherId();
                String subscriberId = publisherId.equals(userId) ? targetId : userId;
                String connectionId = ConnectionManager.generateConnectionId(publisherId, subscriberId, streamId);

                // 根据消息类型更新状态和处理 SDP
                switch (type) {
                    case MessageType.OFFER -> {
                        connectionManager.updateConnectionState(connectionId, "connecting");
                        // 从 Offer SDP 中提取 ICE 参数
                        if (msg.getSdp() != null && msg.getSdp() instanceof Map) {
                            Map<?, ?> sdpMap = (Map<?, ?>) msg.getSdp();
                            Object sdpStr = sdpMap.get("sdp");
                            if (sdpStr != null) {
                                String sdp = sdpStr.toString();
                                String remoteUfrag = SdpUtils.extractIceUfrag(sdp);
                                String remotePwd = SdpUtils.extractIcePwd(sdp);

                                // 生成服务器端的 ICE 参数（简化实现，实际应该使用更安全的随机生成）
                                String localUfrag = generateIceUfrag();
                                String localPwd = generateIcePwd();

                                // 设置 ICE 凭证
                                connectionManager.setIceCredentials(connectionId, localUfrag, localPwd, remoteUfrag);
                            }
                        }
                    }
                    case MessageType.ANSWER -> {
                        connectionManager.markSdpExchanged(connectionId);
                    }
                    case MessageType.CANDIDATE -> {
                        connectionManager.incrementCandidateCount(connectionId);
                    }
                }
            }
        }

        // 转发消息（保持原消息内容，更新时间戳）
        SignalMessage forward = SignalMessage.builder()
                .type(type)
                .roomId(roomId)
                .userId(userId)
                .targetId(targetId)
                .streamId(streamId)
                .sdp(msg.getSdp())
                .candidate(msg.getCandidate())
                .timestamp(System.currentTimeMillis())
                .build();

        send(targetUser.getChannel(), forward);

        // 记录转发统计
        mediaForwarder.recordForwarding(forward.toString().length(), 1);

        log.debug("转发 {} 消息: {} -> {} (stream={})", type, userId, targetId, streamId);
    }

    /**
     * 处理媒体状态变更
     */
    private void handleMediaState(ChannelHandlerContext ctx, SignalMessage msg) {
        String roomId = msg.getRoomId();
        String userId = msg.getUserId();

        if (roomId == null || userId == null) {
            return;
        }

        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return;
        }

        LiveUser user = room.getUser(userId);
        if (user == null) {
            return;
        }

        // 更新媒体状态
        if (msg.getData() instanceof Map data) {
            if (MessageType.MUTE.equals(msg.getType())) {
                user.getMediaState().setAudioMuted(Boolean.parseBoolean(data.getOrDefault("muted", String.valueOf(false)).toString()));
            } else if (MessageType.VIDEO.equals(msg.getType())) {
                user.getMediaState().setVideoMuted(Boolean.parseBoolean(data.getOrDefault("muted", false).toString()));
            }
        }

        // 广播媒体状态变更
        broadcastToRoom(room, SignalMessage.builder()
                .type(MessageType.MEDIA_STATE)
                .roomId(roomId)
                .userId(userId)
                .data(user.getMediaState().toMap())
                .timestamp(System.currentTimeMillis())
                .build(), userId);
    }

    // ==================== 工具方法 ====================

    /**
     * 生成 ICE 用户名片段
     * <p>
     * 简化实现，实际应该使用更安全的随机生成算法
     *
     * @return ICE 用户名片段
     */
    private String generateIceUfrag() {
        return Long.toHexString(System.currentTimeMillis() + (long) (Math.random() * 1000));
    }

    /**
     * 生成 ICE 密码
     * <p>
     * 简化实现，实际应该使用更安全的随机生成算法
     *
     * @return ICE 密码
     */
    private String generateIcePwd() {
        return Long.toHexString(System.currentTimeMillis() + (long) (Math.random() * 10000)) +
                Long.toHexString(System.nanoTime());
    }

    /**
     * 构建 ICE 服务器配置
     */
    private List<Map<String, Object>> buildIceServers() {
        LiveProperties.IceConfig ice = liveProperties.getIce();
        List<Map<String, Object>> servers = new java.util.ArrayList<>();

        // STUN 服务器
        if (ice.getStunServers() != null && !ice.getStunServers().isEmpty()) {
            servers.add(Map.of("urls", ice.getStunServers()));
        }

        // TURN 服务器
        if (ice.getTurnServers() != null) {
            for (LiveProperties.TurnServer turn : ice.getTurnServers()) {
                servers.add(Map.of(
                        "urls", turn.getUrl(),
                        "username", turn.getUsername(),
                        "credential", turn.getCredential()
                ));
            }
        }

        return servers;
    }

    /**
     * 发送消息到 Channel
     */
    private void send(Channel channel, SignalMessage msg) {
        if (channel.isActive()) {
            try {
                String json = objectMapper.writeValueAsString(msg);
                channel.writeAndFlush(new TextWebSocketFrame(json));
            } catch (JsonProcessingException e) {
                log.error("消息序列化失败", e);
            }
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(Channel channel, String type, int code, String message) {
        send(channel, SignalMessage.error(type, code, message));
    }

    /**
     * 广播消息到房间所有成员
     *
     * @param room      房间
     * @param msg       消息
     * @param excludeId 排除的用户 ID
     */
    private void broadcastToRoom(LiveRoom room, SignalMessage msg, String excludeId) {
        try {
            String json = objectMapper.writeValueAsString(msg);
            TextWebSocketFrame frame = new TextWebSocketFrame(json);

            for (LiveUser user : room.getUsers().values()) {
                if (excludeId == null || !excludeId.equals(user.getUserId())) {
                    if (user.getChannel().isActive()) {
                        user.getChannel().writeAndFlush(frame.retainedDuplicate());
                    }
                }
            }
            frame.release();
        } catch (JsonProcessingException e) {
            log.error("广播消息序列化失败", e);
        }
    }
}

