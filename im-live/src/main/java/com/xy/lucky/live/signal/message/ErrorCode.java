package com.xy.lucky.live.signal.message;

/**
 * 错误码常量
 * <p>
 * 定义信令处理过程中可能出现的错误码
 *
 * @author lucky
 */
public final class ErrorCode {

    /**
     * 成功
     */
    public static final int SUCCESS = 0;

    // ==================== 成功 ====================
    /**
     * 未知错误
     */
    public static final int UNKNOWN_ERROR = 1000;

    // ==================== 通用错误 (1000-1999) ====================
    /**
     * 参数错误
     */
    public static final int INVALID_PARAM = 1001;
    /**
     * 消息格式错误
     */
    public static final int INVALID_MESSAGE = 1002;
    /**
     * 未授权
     */
    public static final int UNAUTHORIZED = 1003;
    /**
     * 服务器内部错误
     */
    public static final int INTERNAL_ERROR = 1004;
    /**
     * 操作超时
     */
    public static final int TIMEOUT = 1005;
    /**
     * 房间不存在
     */
    public static final int ROOM_NOT_FOUND = 2000;

    // ==================== 房间相关错误 (2000-2999) ====================
    /**
     * 房间已满
     */
    public static final int ROOM_FULL = 2001;
    /**
     * 已在房间中
     */
    public static final int ALREADY_IN_ROOM = 2002;
    /**
     * 不在房间中
     */
    public static final int NOT_IN_ROOM = 2003;
    /**
     * 房间创建失败
     */
    public static final int ROOM_CREATE_FAILED = 2004;
    /**
     * 用户不存在
     */
    public static final int USER_NOT_FOUND = 3000;

    // ==================== 用户相关错误 (3000-3999) ====================
    /**
     * 用户 ID 冲突
     */
    public static final int USER_ID_CONFLICT = 3001;
    /**
     * 用户已离线
     */
    public static final int USER_OFFLINE = 3002;
    /**
     * 流不存在
     */
    public static final int STREAM_NOT_FOUND = 4000;

    // ==================== 流相关错误 (4000-4999) ====================
    /**
     * 流已存在
     */
    public static final int STREAM_EXISTS = 4001;
    /**
     * 推流数量超限
     */
    public static final int PUBLISH_LIMIT_EXCEEDED = 4002;
    /**
     * 无权限推流
     */
    public static final int PUBLISH_NOT_ALLOWED = 4003;
    /**
     * 无权限订阅
     */
    public static final int SUBSCRIBE_NOT_ALLOWED = 4004;
    /**
     * 流 ID 冲突
     */
    public static final int STREAM_ID_CONFLICT = 4005;
    /**
     * SDP 解析失败
     */
    public static final int SDP_PARSE_ERROR = 5000;

    // ==================== 信令相关错误 (5000-5999) ====================
    /**
     * ICE Candidate 解析失败
     */
    public static final int ICE_PARSE_ERROR = 5001;
    /**
     * 信令交换失败
     */
    public static final int SIGNALING_ERROR = 5002;
    /**
     * 目标用户不存在或不可达
     */
    public static final int TARGET_UNREACHABLE = 5003;

    private ErrorCode() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取错误描述
     *
     * @param code 错误码
     * @return 错误描述
     */
    public static String getMessage(int code) {
        return switch (code) {
            case SUCCESS -> "成功";
            case UNKNOWN_ERROR -> "未知错误";
            case INVALID_PARAM -> "参数错误";
            case INVALID_MESSAGE -> "消息格式错误";
            case UNAUTHORIZED -> "未授权";
            case INTERNAL_ERROR -> "服务器内部错误";
            case TIMEOUT -> "操作超时";
            case ROOM_NOT_FOUND -> "房间不存在";
            case ROOM_FULL -> "房间已满";
            case ALREADY_IN_ROOM -> "已在房间中";
            case NOT_IN_ROOM -> "不在房间中";
            case ROOM_CREATE_FAILED -> "房间创建失败";
            case USER_NOT_FOUND -> "用户不存在";
            case USER_ID_CONFLICT -> "用户ID冲突";
            case USER_OFFLINE -> "用户已离线";
            case STREAM_NOT_FOUND -> "流不存在";
            case STREAM_EXISTS -> "流已存在";
            case PUBLISH_LIMIT_EXCEEDED -> "推流数量超限";
            case PUBLISH_NOT_ALLOWED -> "无权限推流";
            case SUBSCRIBE_NOT_ALLOWED -> "无权限订阅";
            case STREAM_ID_CONFLICT -> "流ID冲突";
            case SDP_PARSE_ERROR -> "SDP解析失败";
            case ICE_PARSE_ERROR -> "ICE Candidate解析失败";
            case SIGNALING_ERROR -> "信令交换失败";
            case TARGET_UNREACHABLE -> "目标用户不可达";
            default -> "未知错误";
        };
    }
}

