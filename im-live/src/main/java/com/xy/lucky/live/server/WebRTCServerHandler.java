package com.xy.lucky.live.server;

import com.xy.lucky.live.core.sfu.SfuService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebRTC UDP 数据包处理器
 * <p>
 * 负责接收和分发 UDP 数据包到 SfuService 进行处理。
 * 这是 WebRTC 媒体流的第一层处理，所有 UDP 流量都会经过这里。
 *
 * <h2>处理流程</h2>
 * <pre>
 * UDP 数据包到达
 *     │
 *     ▼
 * WebRTCServerHandler.channelRead0()
 *     │
 *     ▼
 * 根据数据包类型分发
 *     │
 *     ├─► STUN (0-1) ──► SfuService.handleStun()
 *     ├─► DTLS (20-63) ──► SfuService.handleDtls()
 *     └─► RTP/RTCP (128-191) ──► SfuService.handleRtp() / handleRtcp()
 * </pre>
 *
 * <h2>协议识别</h2>
 * <p>
 * 根据数据包的第一个字节识别协议类型：
 * <ul>
 *   <li><b>0-1</b>: STUN 协议（ICE 绑定请求/响应）</li>
 *   <li><b>20-63</b>: DTLS 协议（TLS over UDP，用于 SRTP 密钥交换）</li>
 *   <li><b>128-191</b>: RTP/RTCP 协议（音视频数据和控制）</li>
 * </ul>
 *
 * <h2>性能考虑</h2>
 * <ul>
 *   <li>使用 Netty 的零拷贝机制，避免不必要的内存复制</li>
 *   <li>快速协议识别，减少处理延迟</li>
 *   <li>异常处理确保单个数据包错误不影响整体服务</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 * @see SfuService
 * @see WebRTCServer
 */
public class WebRTCServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger log = LoggerFactory.getLogger(WebRTCServerHandler.class);

    /**
     * SFU 服务，负责实际的数据包处理
     */
    private final SfuService sfuService;

    /**
     * 统计：接收到的数据包总数
     */
    private long totalPackets = 0;

    /**
     * 统计：STUN 包数量
     */
    private long stunPackets = 0;

    /**
     * 统计：DTLS 包数量
     */
    private long dtlsPackets = 0;

    /**
     * 统计：RTP 包数量
     */
    private long rtpPackets = 0;

    /**
     * 统计：RTCP 包数量
     */
    private long rtcpPackets = 0;

    /**
     * 统计：未知协议包数量
     */
    private long unknownPackets = 0;

    /**
     * 构造函数
     *
     * @param sfuService SFU 服务实例
     */
    public WebRTCServerHandler(SfuService sfuService) {
        this.sfuService = sfuService;
    }

    /**
     * 处理接收到的 UDP 数据包
     * <p>
     * 根据数据包的第一个字节快速识别协议类型，然后分发到相应的处理器。
     *
     * @param ctx    Channel 上下文
     * @param packet UDP 数据包
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        try {
            totalPackets++;

            // 快速协议识别：检查第一个字节
            if (!packet.content().isReadable()) {
                return;
            }

            int firstByte = packet.content().getUnsignedByte(packet.content().readerIndex());

            // 分发到 SfuService 处理
            sfuService.handlePacket(ctx, packet);

            // 统计（仅用于调试，生产环境可关闭）
            if (log.isTraceEnabled()) {
                if (firstByte == 0 || firstByte == 1) {
                    stunPackets++;
                } else if (firstByte >= 20 && firstByte <= 63) {
                    dtlsPackets++;
                } else if (firstByte >= 128 && firstByte <= 191) {
                    // 需要检查第二个字节区分 RTP 和 RTCP
                    if (packet.content().readableBytes() > 1) {
                        int secondByte = packet.content().getUnsignedByte(packet.content().readerIndex() + 1);
                        int pt = secondByte & 0xFF;
                        if (pt >= 200 && pt <= 210) {
                            rtcpPackets++;
                        } else {
                            rtpPackets++;
                        }
                    }
                } else {
                    unknownPackets++;
                }

                // 每 10000 个包输出一次统计
                if (totalPackets % 10000 == 0) {
                    log.trace("UDP 包统计: 总计={}, STUN={}, DTLS={}, RTP={}, RTCP={}, 未知={}",
                            totalPackets, stunPackets, dtlsPackets, rtpPackets, rtcpPackets, unknownPackets);
                }
            }

        } catch (Exception e) {
            // 单个数据包处理失败不应影响整体服务
            log.warn("处理 UDP 数据包异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("UDP 数据包处理异常详情", e);
            }
        }
    }

    /**
     * 处理异常
     *
     * @param ctx   Channel 上下文
     * @param cause 异常原因
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("WebRTC UDP 处理器异常", cause);
        // 不关闭连接，继续处理其他数据包
    }

    /**
     * Channel 激活时调用
     *
     * @param ctx Channel 上下文
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("WebRTC UDP Channel 已激活: {}", ctx.channel().localAddress());
    }

    /**
     * Channel 断开时调用
     *
     * @param ctx Channel 上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("WebRTC UDP Channel 已断开: {}", ctx.channel().localAddress());
    }
}

