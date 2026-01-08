package com.xy.lucky.live.signal.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC 信令消息基类
 * <p>
 * 所有信令消息都遵循此格式，通过 type 字段区分消息类型
 *
 * @author lucky
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalMessage {

    /**
     * 消息类型
     *
     * @see MessageType
     */
    private String type;

    /**
     * 房间 ID
     */
    private String roomId;

    /**
     * 发送者用户 ID
     */
    private String userId;

    /**
     * 目标用户 ID（点对点消息时使用）
     */
    private String targetId;

    /**
     * 流 ID（推流/拉流时使用）
     */
    private String streamId;

    /**
     * SDP 信息（offer/answer 时使用）
     */
    private Object sdp;

    /**
     * ICE Candidate 信息
     */
    private Object candidate;

    /**
     * 附加数据
     */
    private Object data;

    /**
     * 错误码（响应消息时使用）
     */
    private Integer code;

    /**
     * 错误信息（响应消息时使用）
     */
    private String message;

    /**
     * 消息时间戳
     */
    private Long timestamp;

    /**
     * 创建成功响应
     */
    public static SignalMessage success(String type, String message) {
        return SignalMessage.builder()
                .type(type)
                .code(0)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建成功响应（带数据）
     */
    public static SignalMessage success(String type, String message, Object data) {
        return SignalMessage.builder()
                .type(type)
                .code(0)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误响应
     */
    public static SignalMessage error(String type, int code, String message) {
        return SignalMessage.builder()
                .type(type)
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建 Offer 消息
     */
    public static SignalMessage offer(String roomId, String userId, String targetId, String streamId, Object sdp) {
        return SignalMessage.builder()
                .type(MessageType.OFFER)
                .roomId(roomId)
                .userId(userId)
                .targetId(targetId)
                .streamId(streamId)
                .sdp(sdp)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建 Answer 消息
     */
    public static SignalMessage answer(String roomId, String userId, String targetId, String streamId, Object sdp) {
        return SignalMessage.builder()
                .type(MessageType.ANSWER)
                .roomId(roomId)
                .userId(userId)
                .targetId(targetId)
                .streamId(streamId)
                .sdp(sdp)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建 ICE Candidate 消息
     */
    public static SignalMessage candidate(String roomId, String userId, String targetId, String streamId, Object candidate) {
        return SignalMessage.builder()
                .type(MessageType.CANDIDATE)
                .roomId(roomId)
                .userId(userId)
                .targetId(targetId)
                .streamId(streamId)
                .candidate(candidate)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

