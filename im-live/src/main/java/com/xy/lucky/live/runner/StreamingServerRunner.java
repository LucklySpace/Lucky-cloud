package com.xy.lucky.live.runner;

import com.xy.lucky.live.admin.AdminServer;
import com.xy.lucky.live.config.StreamingConfig;
import com.xy.lucky.live.rtmp.RtmpServer;
import com.xy.lucky.live.webrtc.SignalingServer;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.context.ApplicationArguments;
import com.xy.lucky.spring.context.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用启动 Runner：
 * - 根据配置启动 WebRTC 信令与 RTMP 服务
 * - 支持热关闭与异常兜底
 */
@Component
public class StreamingServerRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StreamingServerRunner.class);

    @Autowired
    private StreamingConfig streamingConfig;

    @Autowired
    private SignalingServer signalingServer;

    @Autowired
    private RtmpServer rtmpServer;

    @Autowired
    private AdminServer adminServer;

    @Override
    public void run(ApplicationArguments args) {
        try {
            signalingServer.start(streamingConfig.getSignalingPort());
            rtmpServer.start(streamingConfig.getRtmpPort());
            adminServer.start(streamingConfig.getAdminPort());
            log.info("Streaming servers started: signaling={}, rtmp={}",
                    streamingConfig.getSignalingPort(),
                    streamingConfig.getRtmpPort());
        } catch (Exception e) {
            log.error("启动流媒体服务失败", e);
        }
    }
}
