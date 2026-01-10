package com.xy.lucky.live.signal.message;

/**
 * 信令消息类型常量
 * <p>
 * 定义 WebRTC 信令交换中使用的所有消息类型
 *
 * @author lucky
 */
public final class MessageType {

    /**
     * 心跳
     */
    public static final String HEARTBEAT = "heartbeat";

    // ==================== 连接管理 ====================
    /**
     * 心跳响应
     */
    public static final String PONG = "pong";
    /**
     * 加入房间
     */
    public static final String JOIN = "join";

    // ==================== 房间管理 ====================
    /**
     * 加入房间成功响应
     */
    public static final String JOINED = "joined";
    /**
     * 离开房间
     */
    public static final String LEAVE = "leave";
    /**
     * 离开房间成功响应
     */
    public static final String LEFT = "left";
    /**
     * 用户加入通知（广播给房间其他成员）
     */
    public static final String USER_JOINED = "user_joined";
    /**
     * 用户离开通知（广播给房间其他成员）
     */
    public static final String USER_LEFT = "user_left";
    /**
     * 获取房间信息
     */
    public static final String ROOM_INFO = "room_info";
    /**
     * 开始推流
     */
    public static final String PUBLISH = "publish";

    // ==================== 推拉流管理 ====================
    /**
     * 推流成功响应
     */
    public static final String PUBLISHED = "published";
    /**
     * 停止推流
     */
    public static final String UNPUBLISH = "unpublish";
    /**
     * 停止推流成功响应
     */
    public static final String UNPUBLISHED = "unpublished";
    /**
     * 订阅流
     */
    public static final String SUBSCRIBE = "subscribe";
    /**
     * 订阅成功响应
     */
    public static final String SUBSCRIBED = "subscribed";
    /**
     * 取消订阅
     */
    public static final String UNSUBSCRIBE = "unsubscribe";
    /**
     * 取消订阅成功响应
     */
    public static final String UNSUBSCRIBED = "unsubscribed";
    /**
     * 新流发布通知（广播给房间成员）
     */
    public static final String STREAM_PUBLISHED = "stream_published";
    /**
     * 流停止通知（广播给房间成员）
     */
    public static final String STREAM_UNPUBLISHED = "stream_unpublished";
    /**
     * SDP Offer
     */
    public static final String OFFER = "offer";

    // ==================== WebRTC 信令 ====================
    /**
     * SDP Answer
     */
    public static final String ANSWER = "answer";
    /**
     * ICE Candidate
     */
    public static final String CANDIDATE = "candidate";
    /**
     * 错误消息
     */
    public static final String ERROR = "error";

    // ==================== 错误与通知 ====================
    /**
     * 警告消息
     */
    public static final String WARNING = "warning";
    /**
     * 通知消息
     */
    public static final String NOTICE = "notice";
    /**
     * 静音/取消静音
     */
    public static final String MUTE = "mute";

    // ==================== 媒体控制 ====================
    /**
     * 开启/关闭视频
     */
    public static final String VIDEO = "video";
    /**
     * 媒体状态变更通知
     */
    public static final String MEDIA_STATE = "media_state";
    /**
     * 连接状态更新
     */
    public static final String CONNECTION_STATE = "connection_state";

    // ==================== 连接状态 ====================
    /**
     * 连接成功
     */
    public static final String CONNECTED = "connected";
    /**
     * 连接断开
     */
    public static final String DISCONNECTED = "disconnected";
    /**
     * 统计信息请求/响应
     */
    public static final String STATS = "stats";

    // ==================== 统计与监控 ====================
    /**
     * 质量报告
     */
    public static final String QUALITY_REPORT = "quality_report";

    private MessageType() {
        // 私有构造函数，防止实例化
    }
}

