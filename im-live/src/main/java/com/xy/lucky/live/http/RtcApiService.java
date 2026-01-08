package com.xy.lucky.live.http;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.RoomManager;
import com.xy.lucky.live.core.StreamManager;
import com.xy.lucky.live.core.model.LiveRoom;
import com.xy.lucky.live.core.model.LiveStream;
import com.xy.lucky.live.core.sfu.ConnectionManager;
import com.xy.lucky.live.core.sfu.MediaForwarder;
import com.xy.lucky.live.core.sfu.SdpUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RTC API 服务
 * <p>
 * 处理 SRS 兼容的 WebRTC 推拉流 API 请求。
 * 参考 SRS 的接口设计，提供 `/rtc/v1/publish/` 和 `/rtc/v1/play/` 接口。
 *
 * <h2>SRS 兼容接口</h2>
 * <ul>
 *   <li><b>POST /rtc/v1/publish/</b>: WebRTC 推流接口</li>
 *   <li><b>POST /rtc/v1/play/</b>: WebRTC 拉流接口</li>
 * </ul>
 *
 * <h2>流 URL 格式</h2>
 * <pre>
 * webrtc://domain/app/stream
 * </pre>
 * <p>
 * 解析规则：
 * <ul>
 *   <li>app: 映射为 roomId</li>
 *   <li>stream: 映射为 streamId</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 * @see <a href="https://github.com/ossrs/srs/wiki/v4_CN_WebRTC">SRS WebRTC Wiki</a>
 */
@Component
public class RtcApiService {

    private static final Logger log = LoggerFactory.getLogger(RtcApiService.class);

    @Autowired
    private LiveProperties liveProperties;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private StreamManager streamManager;

    @Autowired
    private MediaForwarder mediaForwarder;

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * 解析流 URL
     * <p>
     * 支持格式：webrtc://domain/app/stream
     *
     * @param streamUrl 流 URL
     * @return 解析结果，包含 app 和 stream
     */
    public StreamUrl parseStreamUrl(String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return null;
        }

        // 移除 webrtc:// 前缀
        String url = streamUrl.replaceFirst("^webrtc://", "");

        // 分割：domain/app/stream
        String[] parts = url.split("/");
        if (parts.length < 3) {
            return null;
        }

        String app = parts[1];      // roomId
        String stream = parts[2];   // streamId

        return new StreamUrl(app, stream);
    }

    /**
     * 处理推流请求
     * <p>
     * 参考 SRS 的 `/rtc/v1/publish/` 接口
     *
     * @param api      API 路径（包含流信息）
     * @param sdpOffer SDP Offer
     * @return SDP Answer
     */
    public RtcApiResponse handlePublish(String api, String sdpOffer) {
        try {
            // 解析流 URL
            StreamUrl streamUrl = parseStreamUrl(api);
            if (streamUrl == null) {
                return RtcApiResponse.error(400, "Invalid stream URL format");
            }

            String roomId = streamUrl.getApp();
            String streamId = streamUrl.getStream();
            String userId = "publisher_" + UUID.randomUUID().toString().substring(0, 8);

            log.info("处理推流请求: roomId={}, streamId={}, userId={}", roomId, streamId, userId);

            // 验证 SDP
            if (!SdpUtils.isValidSdp(sdpOffer)) {
                return RtcApiResponse.error(400, "Invalid SDP format");
            }

            // 提取 ICE 参数
            String remoteUfrag = SdpUtils.extractIceUfrag(sdpOffer);
            String remotePwd = SdpUtils.extractIcePwd(sdpOffer);

            // 生成服务器端 ICE 参数
            String localUfrag = generateIceUfrag();
            String localPwd = generateIcePwd();

            // 创建或获取房间
            LiveRoom room = roomManager.getOrCreateRoom(roomId);

            // 发布流
            StreamManager.PublishResult publishResult = streamManager.publish(
                    roomId, userId, streamId, streamId,
                    LiveStream.StreamType.CAMERA, true, true);

            if (!publishResult.isSuccess()) {
                return RtcApiResponse.error(500, "Failed to publish stream: " + publishResult.getError());
            }

            LiveStream stream = publishResult.getStream();

            // 创建转发会话
//            mediaForwarder.createSession(roomId, userId, streamId, stream);

            // 创建连接（推流者使用特殊 ID "sfu-upstream"）
            ConnectionManager.ConnectionInfo conn = connectionManager.createConnection(
                    userId, "sfu-upstream", streamId, null, null);

            // 设置 ICE 凭证
            connectionManager.setIceCredentials(conn.getConnectionId(), localUfrag, localPwd, remoteUfrag);

            // 生成 SDP Answer
            LiveProperties.RtcConfig rtcConfig = liveProperties.getRtc();
            String sdpAnswer = SdpUtils.generateAnswer(
                    sdpOffer,
                    rtcConfig.getCandidateIp(),
                    rtcConfig.getCandidatePort(),
                    localUfrag,
                    localPwd
            );

            if (sdpAnswer == null) {
                return RtcApiResponse.error(500, "Failed to generate SDP Answer");
            }

            // 构建响应（SRS 兼容格式）
            return RtcApiResponse.success(sdpAnswer, Map.of(
                    "sessionid", conn.getConnectionId(),
                    "app", roomId,
                    "stream", streamId
            ));

        } catch (Exception e) {
            log.error("处理推流请求异常", e);
            return RtcApiResponse.error(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 处理拉流请求
     * <p>
     * 参考 SRS 的 `/rtc/v1/play/` 接口
     *
     * @param api      API 路径（包含流信息）
     * @param sdpOffer SDP Offer
     * @return SDP Answer
     */
    public RtcApiResponse handlePlay(String api, String sdpOffer) {
        try {
            // 解析流 URL
            StreamUrl streamUrl = parseStreamUrl(api);
            if (streamUrl == null) {
                return RtcApiResponse.error(400, "Invalid stream URL format");
            }

            String roomId = streamUrl.getApp();
            String streamId = streamUrl.getStream();
            String userId = "subscriber_" + UUID.randomUUID().toString().substring(0, 8);

            log.info("处理拉流请求: roomId={}, streamId={}, userId={}", roomId, streamId, userId);

            // 验证 SDP
            if (!SdpUtils.isValidSdp(sdpOffer)) {
                return RtcApiResponse.error(400, "Invalid SDP format");
            }

            // 检查流是否存在
            LiveRoom room = roomManager.getRoom(roomId);
            if (room == null) {
                return RtcApiResponse.error(404, "Room not found: " + roomId);
            }

            LiveStream stream = room.getStream(streamId);
            if (stream == null) {
                return RtcApiResponse.error(404, "Stream not found: " + streamId);
            }

            // 订阅流
            StreamManager.SubscribeResult subscribeResult = streamManager.subscribe(roomId, userId, streamId);
            if (!subscribeResult.isSuccess()) {
                return RtcApiResponse.error(500, "Failed to subscribe stream: " + subscribeResult.getError());
            }

            // 添加到转发会话
            mediaForwarder.addSubscriber(streamId, userId, null);

            // 提取 ICE 参数
            String remoteUfrag = SdpUtils.extractIceUfrag(sdpOffer);
            String remotePwd = SdpUtils.extractIcePwd(sdpOffer);

            // 生成服务器端 ICE 参数
            String localUfrag = generateIceUfrag();
            String localPwd = generateIcePwd();

            // 创建连接
            String publisherId = stream.getPublisherId();
            ConnectionManager.ConnectionInfo conn = connectionManager.createConnection(
                    publisherId, userId, streamId, null, null);

            // 设置 ICE 凭证
            connectionManager.setIceCredentials(conn.getConnectionId(), localUfrag, localPwd, remoteUfrag);

            // 生成 SDP Answer
            LiveProperties.RtcConfig rtcConfig = liveProperties.getRtc();
            String sdpAnswer = SdpUtils.generateAnswer(
                    sdpOffer,
                    rtcConfig.getCandidateIp(),
                    rtcConfig.getCandidatePort(),
                    localUfrag,
                    localPwd
            );

            if (sdpAnswer == null) {
                return RtcApiResponse.error(500, "Failed to generate SDP Answer");
            }

            // 构建响应（SRS 兼容格式）
            return RtcApiResponse.success(sdpAnswer, Map.of(
                    "sessionid", conn.getConnectionId(),
                    "app", roomId,
                    "stream", streamId
            ));

        } catch (Exception e) {
            log.error("处理拉流请求异常", e);
            return RtcApiResponse.error(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * 生成 ICE 用户名片段
     *
     * @return ICE 用户名片段
     */
    private String generateIceUfrag() {
        return Long.toHexString(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(1000));
    }

    /**
     * 生成 ICE 密码
     *
     * @return ICE 密码
     */
    private String generateIcePwd() {
        return Long.toHexString(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(10000)) +
                Long.toHexString(System.nanoTime());
    }

    /**
     * 流 URL 解析结果
     */
    public static class StreamUrl {
        private final String app;      // roomId
        private final String stream;   // streamId

        public StreamUrl(String app, String stream) {
            this.app = app;
            this.stream = stream;
        }

        public String getApp() {
            return app;
        }

        public String getStream() {
            return stream;
        }
    }
}

