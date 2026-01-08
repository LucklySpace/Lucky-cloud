package com.xy.lucky.live.runner;

import com.xy.lucky.live.config.LiveProperties;
import com.xy.lucky.live.core.RoomManager;
import com.xy.lucky.live.server.SignalingServer;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.boot.context.ApplicationArguments;
import com.xy.lucky.spring.boot.context.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 直播服务启动 Runner
 * <p>
 * 在应用启动完成后执行，输出服务信息和使用说明
 *
 * @author lucky
 */
@Component
public class LiveServerRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LiveServerRunner.class);

    @Autowired
    private SignalingServer signalingServer;

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private LiveProperties liveProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (signalingServer.isRunning()) {
            printUsageGuide();
        } else {
            log.error("信令服务器启动失败！");
        }
    }

    /**
     * 打印使用指南
     */
    private void printUsageGuide() {
        String wsUrl = String.format("ws://localhost:%d%s",
                liveProperties.getSignaling().getPort(),
                liveProperties.getSignaling().getPath());

        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════════════╗");
        log.info("║                        im-live 使用指南                               ║");
        log.info("╠══════════════════════════════════════════════════════════════════════╣");
        log.info("║                                                                      ║");
        log.info("║  WebSocket 信令地址: {}                          ", wsUrl);

        // HTTP API 地址
        if (liveProperties.getHttpApi().isEnabled()) {
            String httpApiUrl = String.format("http://localhost:%d%s",
                    liveProperties.getHttpApi().getPort(),
                    liveProperties.getHttpApi().getApiBase());
            log.info("║  HTTP API 地址: {}                                    ", httpApiUrl);
            log.info("║    - 推流: POST {}/publish/                            ", httpApiUrl);
            log.info("║    - 拉流: POST {}/play/                              ", httpApiUrl);
        }

        log.info("║                                                                      ║");
        log.info("║  信令消息格式（JSON）:                                                ║");
        log.info("║                                                                      ║");
        log.info("║  1. 加入房间:                                                         ║");
        log.info("║     {{\"type\":\"join\",\"roomId\":\"room1\",\"userId\":\"user1\"}}             ║");
        log.info("║                                                                      ║");
        log.info("║  2. 发布流:                                                           ║");
        log.info("║     {{\"type\":\"publish\",\"roomId\":\"room1\",\"userId\":\"user1\",           ║");
        log.info("║       \"streamId\":\"stream1\"}}                                        ║");
        log.info("║                                                                      ║");
        log.info("║  3. 订阅流:                                                           ║");
        log.info("║     {{\"type\":\"subscribe\",\"roomId\":\"room1\",\"userId\":\"user2\",         ║");
        log.info("║       \"streamId\":\"stream1\"}}                                        ║");
        log.info("║                                                                      ║");
        log.info("║  4. SDP Offer:                                                       ║");
        log.info("║     {{\"type\":\"offer\",\"roomId\":\"room1\",\"userId\":\"user1\",             ║");
        log.info("║       \"targetId\":\"user2\",\"streamId\":\"stream1\",\"sdp\":{{...}}}}       ║");
        log.info("║                                                                      ║");
        log.info("║  5. ICE Candidate:                                                   ║");
        log.info("║     {{\"type\":\"candidate\",\"roomId\":\"room1\",\"userId\":\"user1\",         ║");
        log.info("║       \"targetId\":\"user2\",\"streamId\":\"stream1\",\"candidate\":{{...}}}}  ║");
        log.info("║                                                                      ║");
        log.info("║  测试页面:                                                           ║");
        log.info("║    - WebSocket: resources/static/index.html                          ║");
        log.info("║    - HTTP API (SRS 兼容): resources/static/webrtc.html             ║");
        log.info("║                                                                      ║");
        log.info("╚══════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }
}

