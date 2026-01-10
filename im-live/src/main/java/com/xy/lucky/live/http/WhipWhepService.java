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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WHIP/WHEP 服务
 * <p>
 * 处理 WHIP (WebRTC-HTTP Ingestion Protocol) 推流和 WHEP (WebRTC-HTTP Egress Protocol) 拉流。
 * 参考 SRS 的接口设计，提供基于 HTTP 的 WebRTC 推拉流接口。
 *
 * <h2>WHIP 协议</h2>
 * <ul>
 *   <li>POST /rtc/v1/whip/?app={app}&stream={stream} - 推流，客户端发送 SDP Offer</li>
 *   <li>DELETE /rtc/v1/whip/?app={app}&stream={stream} - 停止推流</li>
 * </ul>
 *
 * <h2>WHEP 协议</h2>
 * <ul>
 *   <li>GET /rtc/v1/whep/?app={app}&stream={stream} - 拉流，服务器返回 SDP Offer</li>
 *   <li>POST /rtc/v1/whep/?app={app}&stream={stream} - 客户端发送 SDP Answer</li>
 * </ul>
 *
 * <h2>流 URL 格式</h2>
 * <pre>
 * 推流: webrtc://localhost:8082/live/stream1
 * 拉流: webrtc://localhost:8082/live/stream1
 * </pre>
 *
 * @author lucky
 * @version 1.0.0
 * @see <a href="https://datatracker.ietf.org/doc/draft-ietf-wish-whip/">WHIP Draft</a>
 */
@Component
public class WhipWhepService {

    private static final Logger log = LoggerFactory.getLogger(WhipWhepService.class);

    /**
     * 流 ID -> 推流连接信息映射
     * 用于 WHIP 推流时存储连接信息
     */
    private final ConcurrentHashMap<String, WhipConnection> whipConnections = new ConcurrentHashMap<>();

    /**
     * 流 ID -> 拉流连接信息映射
     * 用于 WHEP 拉流时存储连接信息
     */
    private final ConcurrentHashMap<String, WhepConnection> whepConnections = new ConcurrentHashMap<>();

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
     * 处理 WHIP 推流请求
     * <p>
     * 客户端发送 SDP Offer，服务器生成 SDP Answer 并返回。
     *
     * @param app      应用名称（对应房间 ID）
     * @param stream   流名称（对应流 ID）
     * @param offerSdp SDP Offer
     * @return SDP Answer
     */
    public String handleWhipPublish(String app, String stream, String offerSdp) {
        if (app == null || app.isEmpty()) {
            app = "live"; // 默认应用
        }
        if (stream == null || stream.isEmpty()) {
            throw new IllegalArgumentException("流名称不能为空");
        }

        String roomId = app;
        String streamId = stream;
        String publisherId = "whip_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("WHIP 推流请求: app={}, stream={}, publisherId={}", app, stream, publisherId);

        // 验证 SDP
        if (!SdpUtils.isValidSdp(offerSdp)) {
            throw new IllegalArgumentException("无效的 SDP 格式");
        }

        // 提取 ICE 参数
        String remoteUfrag = SdpUtils.extractIceUfrag(offerSdp);
        String remotePwd = SdpUtils.extractIcePwd(offerSdp);

        // 生成服务器端 ICE 参数
        String localUfrag = generateIceUfrag();
        String localPwd = generateIcePwd();

        // 获取 RTC 配置
        LiveProperties.RtcConfig rtc = liveProperties.getRtc();
        String candidateIp = rtc.getCandidateIp();
        int candidatePort = rtc.getCandidatePort();

        // 生成 SDP Answer
        String answerSdp = SdpUtils.generateAnswer(offerSdp, candidateIp, candidatePort, localUfrag, localPwd);

        // 创建或加入房间
        RoomManager.JoinResult joinResult = roomManager.joinRoom(roomId, publisherId, null, "WHIP Publisher");
        if (!joinResult.isSuccess()) {
            throw new RuntimeException("加入房间失败: " + joinResult.getError());
        }

        // 发布流
        StreamManager.PublishResult publishResult = streamManager.publish(
                roomId, publisherId, streamId, streamId, LiveStream.StreamType.CAMERA, true, true);
        if (!publishResult.isSuccess()) {
            throw new RuntimeException("发布流失败: " + publishResult.getError());
        }

        LiveStream liveStream = publishResult.getStream();

        // 创建转发会话
        MediaForwarder mediaForwarder = this.mediaForwarder;
        mediaForwarder.createSession(roomId, publisherId, streamId, liveStream);

        // 创建连接（推流者使用特殊 ID "sfu-upstream"）
        ConnectionManager.ConnectionInfo conn = connectionManager.createConnection(
                publisherId, "sfu-upstream", streamId, null, null);
        connectionManager.setIceCredentials(conn.getConnectionId(), localUfrag, localPwd, remoteUfrag);

        // 保存 WHIP 连接信息
        WhipConnection whipConn = new WhipConnection();
        whipConn.setRoomId(roomId);
        whipConn.setStreamId(streamId);
        whipConn.setPublisherId(publisherId);
        whipConn.setConnectionId(conn.getConnectionId());
        whipConnections.put(streamId, whipConn);

        log.info("WHIP 推流成功: streamId={}, publisherId={}", streamId, publisherId);

        return answerSdp;
    }

    /**
     * 停止 WHIP 推流
     *
     * @param app    应用名称
     * @param stream 流名称
     */
    public void handleWhipUnpublish(String app, String stream) {
        if (app == null || app.isEmpty()) {
            app = "live";
        }

        WhipConnection conn = whipConnections.remove(stream);
        if (conn != null) {
            // 停止发布流
            streamManager.unpublish(conn.getRoomId(), conn.getPublisherId(), conn.getStreamId());
            // 销毁转发会话
            mediaForwarder.destroySession(conn.getStreamId());
            // 移除连接
            connectionManager.removeConnection(conn.getConnectionId());
            log.info("WHIP 推流已停止: streamId={}", conn.getStreamId());
        }
    }

    /**
     * 处理 WHEP 拉流请求
     * <p>
     * 服务器生成 SDP Offer 并返回给客户端。
     *
     * @param app    应用名称
     * @param stream 流名称
     * @return SDP Offer
     */
    public String handleWhepPlay(String app, String stream) {
        if (app == null || app.isEmpty()) {
            app = "live";
        }
        if (stream == null || stream.isEmpty()) {
            throw new IllegalArgumentException("流名称不能为空");
        }

        String roomId = app;
        String streamId = stream;

        log.info("WHEP 拉流请求: app={}, stream={}", app, stream);

        // 检查流是否存在
        LiveRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            throw new RuntimeException("房间不存在: " + roomId);
        }

        LiveStream liveStream = room.getStream(streamId);
        if (liveStream == null) {
            throw new RuntimeException("流不存在: " + streamId);
        }

        String subscriberId = "whep_" + UUID.randomUUID().toString().substring(0, 8);

        // 生成服务器端 ICE 参数
        String localUfrag = generateIceUfrag();
        String localPwd = generateIcePwd();

        // 获取 RTC 配置
        LiveProperties.RtcConfig rtc = liveProperties.getRtc();
        String candidateIp = rtc.getCandidateIp();
        int candidatePort = rtc.getCandidatePort();

        // 生成 SDP Offer（简化实现，实际应该根据流的编码格式生成）
        String offerSdp = generateWhepOffer(candidateIp, candidatePort, localUfrag, localPwd);

        // 订阅流
        StreamManager.SubscribeResult subscribeResult = streamManager.subscribe(roomId, subscriberId, streamId);
        if (!subscribeResult.isSuccess()) {
            throw new RuntimeException("订阅流失败: " + subscribeResult.getError());
        }

        // 添加到转发会话
        mediaForwarder.addSubscriber(streamId, subscriberId, null);

        // 创建连接
        String publisherId = liveStream.getPublisherId();
        ConnectionManager.ConnectionInfo conn = connectionManager.createConnection(
                publisherId, subscriberId, streamId, null, null);
        connectionManager.setIceCredentials(conn.getConnectionId(), localUfrag, localPwd, null);

        // 保存 WHEP 连接信息
        WhepConnection whepConn = new WhepConnection();
        whepConn.setRoomId(roomId);
        whepConn.setStreamId(streamId);
        whepConn.setSubscriberId(subscriberId);
        whepConn.setPublisherId(publisherId);
        whepConn.setConnectionId(conn.getConnectionId());
        whepConnections.put(streamId + "_" + subscriberId, whepConn);

        log.info("WHEP 拉流成功: streamId={}, subscriberId={}", streamId, subscriberId);

        return offerSdp;
    }

    /**
     * 处理 WHEP Answer
     * <p>
     * 客户端发送 SDP Answer，完成连接建立。
     *
     * @param app       应用名称
     * @param stream    流名称
     * @param answerSdp SDP Answer
     */
    public void handleWhepAnswer(String app, String stream, String answerSdp) {
        if (app == null || app.isEmpty()) {
            app = "live";
        }

        // 验证 SDP
        if (!SdpUtils.isValidSdp(answerSdp)) {
            throw new IllegalArgumentException("无效的 SDP 格式");
        }

        // 提取 ICE 参数
        String remoteUfrag = SdpUtils.extractIceUfrag(answerSdp);
        String remotePwd = SdpUtils.extractIcePwd(answerSdp);

        // 查找连接并更新
        // 这里简化处理，实际应该通过其他方式标识连接
        log.info("WHEP Answer 接收: app={}, stream={}", app, stream);
    }

    /**
     * 生成 WHEP SDP Offer
     *
     * @param candidateIp   候选 IP
     * @param candidatePort 候选端口
     * @param iceUfrag      ICE 用户名片段
     * @param icePwd        ICE 密码
     * @return SDP Offer
     */
    private String generateWhepOffer(String candidateIp, int candidatePort, String iceUfrag, String icePwd) {
        StringBuilder sdp = new StringBuilder();
        long sessionId = System.currentTimeMillis();

        // 会话级描述
        sdp.append("v=0\r\n");
        sdp.append("o=- ").append(sessionId).append(" ").append(sessionId).append(" IN IP4 ").append(candidateIp).append("\r\n");
        sdp.append("s=-\r\n");
        sdp.append("t=0 0\r\n");
        sdp.append("c=IN IP4 ").append(candidateIp).append("\r\n");

        // 会话级 ICE 参数
        sdp.append("a=ice-ufrag:").append(iceUfrag).append("\r\n");
        sdp.append("a=ice-pwd:").append(icePwd).append("\r\n");
        sdp.append("a=fingerprint:sha-256 00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00\r\n");
        sdp.append("a=setup:actpass\r\n");

        // 音频媒体描述
        sdp.append("m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n");
        sdp.append("c=IN IP4 ").append(candidateIp).append("\r\n");
        sdp.append("a=ice-ufrag:").append(iceUfrag).append("\r\n");
        sdp.append("a=ice-pwd:").append(icePwd).append("\r\n");
        sdp.append("a=setup:actpass\r\n");
        sdp.append("a=mid:audio\r\n");
        sdp.append("a=recvonly\r\n");
        sdp.append("a=rtpmap:111 opus/48000/2\r\n");
        sdp.append("a=candidate:foundation 1 UDP 2130706431 ").append(candidateIp).append(" ").append(candidatePort).append(" typ host\r\n");

        // 视频媒体描述
        sdp.append("m=video 9 UDP/TLS/RTP/SAVPF 96 97\r\n");
        sdp.append("c=IN IP4 ").append(candidateIp).append("\r\n");
        sdp.append("a=ice-ufrag:").append(iceUfrag).append("\r\n");
        sdp.append("a=ice-pwd:").append(icePwd).append("\r\n");
        sdp.append("a=setup:actpass\r\n");
        sdp.append("a=mid:video\r\n");
        sdp.append("a=recvonly\r\n");
        sdp.append("a=rtpmap:96 H264/90000\r\n");
        sdp.append("a=rtpmap:97 VP8/90000\r\n");
        sdp.append("a=candidate:foundation 1 UDP 2130706431 ").append(candidateIp).append(" ").append(candidatePort).append(" typ host\r\n");

        return sdp.toString();
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
     * WHIP 连接信息
     */
    @lombok.Data
    public static class WhipConnection {
        private String roomId;
        private String streamId;
        private String publisherId;
        private String connectionId;
    }

    /**
     * WHEP 连接信息
     */
    @lombok.Data
    public static class WhepConnection {
        private String roomId;
        private String streamId;
        private String subscriberId;
        private String publisherId;
        private String connectionId;
    }
}

