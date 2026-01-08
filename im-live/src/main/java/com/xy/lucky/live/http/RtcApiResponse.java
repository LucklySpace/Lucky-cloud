package com.xy.lucky.live.http;

import lombok.Data;

import java.util.Map;

/**
 * RTC API 响应
 * <p>
 * SRS 兼容的响应格式
 *
 * @author lucky
 */
@Data
public class RtcApiResponse {

    /**
     * 响应码（0 表示成功）
     */
    private int code;

    /**
     * 响应消息
     */
    private String server;

    /**
     * SDP Answer（成功时）
     */
    private String sdp;

    /**
     * 会话 ID
     */
    private String sessionid;

    /**
     * 应用名称（roomId）
     */
    private String app;

    /**
     * 流名称（streamId）
     */
    private String stream;

    /**
     * 其他数据
     */
    private Map<String, Object> data;

    /**
     * 创建成功响应
     *
     * @param sdp  SDP Answer
     * @param data 附加数据
     * @return 响应对象
     */
    public static RtcApiResponse success(String sdp, Map<String, Object> data) {
        RtcApiResponse response = new RtcApiResponse();
        response.setCode(0);
        response.setServer("im-live/1.0.0");
        response.setSdp(sdp);

        if (data != null) {
            if (data.containsKey("sessionid")) {
                response.setSessionid(data.get("sessionid").toString());
            }
            if (data.containsKey("app")) {
                response.setApp(data.get("app").toString());
            }
            if (data.containsKey("stream")) {
                response.setStream(data.get("stream").toString());
            }
            response.setData(data);
        }

        return response;
    }

    /**
     * 创建错误响应
     *
     * @param code    错误码
     * @param message 错误消息
     * @return 响应对象
     */
    public static RtcApiResponse error(int code, String message) {
        RtcApiResponse response = new RtcApiResponse();
        response.setCode(code);
        response.setServer("im-live/1.0.0");
        response.setData(Map.of("error", message));
        return response;
    }
}

