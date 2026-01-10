package com.xy.lucky.live.core.sfu;

import com.xy.lucky.live.core.rtp.RtcpPacket;
import com.xy.lucky.live.core.rtp.RtpPacket;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SFU (Selective Forwarding Unit) 核心服务
 * <p>
 * 这是中心化 WebRTC 流媒体服务器的核心组件，负责处理所有 UDP 数据包的分发和转发。
 * 参考 SRS (Simple Realtime Server) 的 WebRTC 架构设计实现。
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li><b>STUN 处理</b>: 处理 ICE 绑定请求，建立连接和地址映射</li>
 *   <li><b>DTLS 处理</b>: 处理 DTLS 握手（待完善），生成 SRTP 密钥</li>
 *   <li><b>RTP 转发</b>: 将推流者的 RTP 包转发给所有订阅者</li>
 *   <li><b>RTCP 处理</b>: 处理 PLI/FIR 请求，转发给推流者以请求关键帧</li>
 * </ul>
 *
 * <h2>数据流向</h2>
 * <pre>
 * 推流者 ──UDP──► SfuService ──► 解析协议类型
 *                                    │
 *                    ┌───────────────┼───────────────┐
 *                    ▼               ▼               ▼
 *               STUN 处理      DTLS 处理      RTP/RTCP 处理
 *                    │               │               │
 *                    │               │               ▼
 *                    │               │         查找转发会话
 *                    │               │               │
 *                    │               │               ▼
 *                    │               │         复制 RTP 包
 *                    │               │               │
 *                    │               │               ▼
 *                    │               │         转发给订阅者
 *                    │               │               │
 *                    └───────────────┴───────────────┘
 *                                    │
 *                                    ▼
 *                              订阅者 1, 2, 3...
 * </pre>
 *
 * <h2>协议识别</h2>
 * <p>
 * 根据数据包的第一个字节快速识别协议：
 * <ul>
 *   <li><b>0-1</b>: STUN 协议（ICE 连接建立）</li>
 *   <li><b>20-63</b>: DTLS 协议（SRTP 密钥交换）</li>
 *   <li><b>128-191</b>: RTP/RTCP 协议（媒体数据和控制）</li>
 * </ul>
 *
 * <h2>地址映射</h2>
 * <p>
 * 通过 STUN Binding Request 建立客户端地址到连接 ID 的映射：
 * <pre>
 * 客户端地址 (IP:Port) ──► Connection ID ──► 连接信息
 * </pre>
 * 这样在收到 RTP 包时，可以通过发送者地址快速找到对应的连接和流。
 *
 * @author lucky
 * @version 1.0.0
 * @see MediaForwarder
 * @see ConnectionManager
 * @see <a href="https://github.com/ossrs/srs/wiki/v4_CN_WebRTC">SRS WebRTC Wiki</a>
 */
@Component
public class SfuService {

    private static final Logger log = LoggerFactory.getLogger(SfuService.class);

    /**
     * STUN Magic Cookie (RFC 5389)
     */
    private static final int STUN_MAGIC_COOKIE = 0x2112A442;

    /**
     * STUN 消息类型
     */
    private static final int STUN_BINDING_REQUEST = 0x0001;
    private static final int STUN_BINDING_RESPONSE = 0x0101;

    /**
     * STUN 属性类型
     */
    private static final int STUN_ATTR_USERNAME = 0x0006;
    private static final int STUN_ATTR_MESSAGE_INTEGRITY = 0x0008;
    private static final int STUN_ATTR_XOR_MAPPED_ADDRESS = 0x0020;

    /**
     * 地址 -> 连接 ID 映射
     * <p>
     * 通过 STUN Binding Request 建立映射关系。
     * 当客户端发送 STUN 请求时，服务器记录客户端地址对应的连接 ID。
     */
    private final Map<InetSocketAddress, String> addressToConnectionId = new ConcurrentHashMap<>();

    @Autowired
    private MediaForwarder mediaForwarder;

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * 处理 UDP 数据包
     * <p>
     * 根据数据包的第一个字节快速识别协议类型，然后分发到相应的处理器。
     *
     * @param ctx    Netty 上下文
     * @param packet UDP 数据包
     */
    public void handlePacket(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf content = packet.content();
        if (!content.isReadable()) {
            return;
        }

        int firstByte = content.getUnsignedByte(content.readerIndex());

        // 1. STUN 协议 (0-1)
        // STUN 消息的第一个字节通常是 0x00 或 0x01（消息类型的高位）
        if (firstByte == 0 || firstByte == 1) {
            handleStun(ctx, packet);
            return;
        }

        // 2. DTLS 协议 (20-63)
        // DTLS 记录层的 ContentType 范围是 20-63
        if (firstByte >= 20 && firstByte <= 63) {
            handleDtls(ctx, packet);
            return;
        }

        // 3. RTP/RTCP 协议 (128-191)
        // RTP 版本号是 2，第一个字节的高 2 位是 10 (二进制)，即 128-191
        if (firstByte >= 128 && firstByte <= 191) {
            byte secondByte = content.getByte(content.readerIndex() + 1);
            int pt = secondByte & 0xFF;

            // RTCP 负载类型范围：200-210
            if (pt >= 200 && pt <= 210) {
                handleRtcp(ctx, packet);
            } else {
                handleRtp(ctx, packet);
            }
        }
    }

    /**
     * 处理 STUN 包
     * <p>
     * STUN (Session Traversal Utilities for NAT) 用于 ICE 连接建立。
     * 客户端发送 Binding Request，服务器验证后回复 Binding Response。
     *
     * <h2>STUN 消息格式</h2>
     * <pre>
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |0 0|     STUN Message Type     |         Message Length         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         Magic Cookie                          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                     Transaction ID (96 bits)                  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     *
     * @param ctx    Netty 上下文
     * @param packet UDP 数据包
     */
    private void handleStun(ChannelHandlerContext ctx, DatagramPacket packet) {
        ByteBuf buf = packet.content();
        int startIndex = buf.readerIndex();

        try {
            // 读取 STUN 消息头
            int type = buf.readUnsignedShort();
            int length = buf.readUnsignedShort();

            // 验证 Magic Cookie
            int cookie = buf.readInt();
            if (cookie != STUN_MAGIC_COOKIE) {
                // 不是有效的 STUN 消息
                return;
            }

            // 读取 Transaction ID
            byte[] transactionId = new byte[12];
            buf.readBytes(transactionId);

            // 处理 Binding Request
            if (type == STUN_BINDING_REQUEST) {
                // 解析 Attributes 寻找 USERNAME
                String username = null;
                while (buf.readableBytes() >= 4) {
                    int attrType = buf.readUnsignedShort();
                    int attrLen = buf.readUnsignedShort();

                    // 4 字节对齐
                    int padding = (4 - (attrLen % 4)) % 4;

                    if (attrType == STUN_ATTR_USERNAME) {
                        byte[] val = new byte[attrLen];
                        buf.readBytes(val);
                        username = new String(val, StandardCharsets.UTF_8);
                        buf.skipBytes(padding);
                    } else {
                        buf.skipBytes(attrLen + padding);
                    }
                }

                if (username != null) {
                    // USERNAME 格式：remote_ufrag:local_ufrag
                    // 在客户端发送的请求中：ServerUfrag:ClientUfrag
                    String[] parts = username.split(":");
                    if (parts.length >= 2) {
                        // 尝试匹配连接
                        ConnectionManager.ConnectionInfo conn = connectionManager.getConnectionByUfrag(parts[1]);
                        if (conn == null) {
                            conn = connectionManager.getConnectionByUfrag(parts[0]);
                        }

                        if (conn != null) {
                            // 更新地址映射
                            InetSocketAddress sender = packet.sender();
                            addressToConnectionId.put(sender, conn.getConnectionId());
                            conn.setRemoteAddress(sender);

                            // 更新 ICE 连接状态
                            if (!"connected".equals(conn.getIceState())) {
                                conn.setIceState("connected");
                                connectionManager.updateConnectionState(conn.getConnectionId(), "connected");
                                log.info("ICE 连接建立: connId={}, addr={}", conn.getConnectionId(), sender);
                            }

                            // 发送 Binding Response
                            sendStunBindingResponse(ctx, packet.sender(), transactionId, conn.getLocalPwd());
                        } else {
                            log.debug("未找到匹配的连接: username={}", username);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("处理 STUN 包异常: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("STUN 包处理异常详情", e);
            }
        } finally {
            // 恢复读取位置（如果需要）
            buf.readerIndex(startIndex);
        }
    }

    /**
     * 发送 STUN Binding Response
     * <p>
     * 回复客户端的 Binding Request，包含 XOR-MAPPED-ADDRESS 和 MESSAGE-INTEGRITY。
     *
     * @param ctx           Netty 上下文
     * @param sender        发送者地址
     * @param transactionId 事务 ID（必须与请求一致）
     * @param password      ICE 密码（用于计算 MESSAGE-INTEGRITY）
     */
    private void sendStunBindingResponse(ChannelHandlerContext ctx, InetSocketAddress sender,
                                         byte[] transactionId, String password) {
        ByteBuf response = Unpooled.buffer(1024);

        try {
            // Header: Type (0x0101 = Binding Response), Length (placeholder), Cookie, TransactionID
            response.writeShort(STUN_BINDING_RESPONSE);
            response.writeShort(0); // 长度占位符
            response.writeInt(STUN_MAGIC_COOKIE);
            response.writeBytes(transactionId);

            // XOR-MAPPED-ADDRESS (0x0020)
            // 这是客户端看到的服务器地址（经过 NAT 后的地址）
            response.writeShort(STUN_ATTR_XOR_MAPPED_ADDRESS);
            response.writeShort(8); // 属性长度
            response.writeByte(0); // Reserved
            response.writeByte(1); // IPv4

            // XOR 编码端口和 IP（RFC 5389 要求）
            int port = sender.getPort();
            int xorPort = port ^ (STUN_MAGIC_COOKIE >> 16);
            response.writeShort(xorPort);

            byte[] address = sender.getAddress().getAddress();
            int ip = (address[0] & 0xFF) << 24 | (address[1] & 0xFF) << 16 |
                    (address[2] & 0xFF) << 8 | (address[3] & 0xFF);
            int xorIp = ip ^ STUN_MAGIC_COOKIE;
            response.writeInt(xorIp);

            // MESSAGE-INTEGRITY (0x0008)
            // 使用 HMAC-SHA1 计算消息完整性
            if (password != null) {
                // 写入 Integrity 属性头（值部分先占位）
                int integrityPos = response.writerIndex();
                response.writeShort(STUN_ATTR_MESSAGE_INTEGRITY);
                response.writeShort(20); // HMAC-SHA1 长度固定为 20 字节
                response.writeZero(20); // 占位符

                // 更新 Header Length（包含 Integrity 属性）
                int totalLength = response.writerIndex() - 20; // 减去 STUN 头部
                response.setShort(2, totalLength);

                // 计算 HMAC-SHA1
                byte[] messageBytes = new byte[response.writerIndex()];
                response.getBytes(0, messageBytes);
                byte[] hmac = calculateHmacSha1(messageBytes, password);

                // 写入 HMAC 值
                response.setBytes(integrityPos + 4, hmac);
            } else {
                // 没有密码，只更新长度
                response.setShort(2, response.writerIndex() - 20);
            }

            // 发送响应
            ctx.writeAndFlush(new DatagramPacket(response, sender));

        } catch (Exception e) {
            log.error("发送 STUN Response 异常", e);
            response.release();
        }
    }

    /**
     * 计算 HMAC-SHA1
     *
     * @param data 数据
     * @param key  密钥
     * @return HMAC-SHA1 值
     */
    private byte[] calculateHmacSha1(byte[] data, String key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            return mac.doFinal(data);
        } catch (Exception e) {
            log.error("HMAC SHA1 计算异常", e);
            return new byte[20];
        }
    }

    /**
     * 处理 DTLS 包
     * <p>
     * DTLS (Datagram Transport Layer Security) 用于 SRTP 密钥交换。
     * 当前实现为占位符，完整的 DTLS 握手需要集成 Bouncy Castle 等库。
     *
     * <h2>DTLS 握手流程</h2>
     * <ol>
     *   <li>ClientHello: 客户端发起握手</li>
     *   <li>ServerHello: 服务器响应</li>
     *   <li>Certificate Exchange: 证书交换</li>
     *   <li>Key Exchange: 密钥交换</li>
     *   <li>Finished: 握手完成</li>
     * </ol>
     *
     * <h2>SRTP 密钥导出</h2>
     * <p>
     * DTLS 握手完成后，使用 DTLS 主密钥导出 SRTP 密钥：
     * <pre>
     * SRTP Master Key = PRF(DTLS Master Secret, "EXTRACTOR-dtls_srtp", ...)
     * </pre>
     *
     * @param ctx    Netty 上下文
     * @param packet UDP 数据包
     */
    private void handleDtls(ChannelHandlerContext ctx, DatagramPacket packet) {
        // TODO: 实现完整的 DTLS 握手
        // 需要：
        // 1. 解析 DTLS 记录层
        // 2. 处理握手消息（ClientHello, ServerHello, Certificate, etc.）
        // 3. 生成服务器证书
        // 4. 完成密钥交换
        // 5. 导出 SRTP 密钥
        // 6. 使用 SRTP 加密/解密 RTP 包

        // 当前实现：仅记录日志
        if (log.isTraceEnabled()) {
            log.trace("收到 DTLS 包: from={}, size={}", packet.sender(), packet.content().readableBytes());
        }
    }

    /**
     * 处理 RTP 包
     * <p>
     * 将推流者发送的 RTP 包转发给所有订阅者。
     * 这是 SFU 的核心功能：选择性转发媒体流。
     *
     * <h2>转发流程</h2>
     * <ol>
     *   <li>通过发送者地址查找连接 ID</li>
     *   <li>获取连接信息，确认是推流者</li>
     *   <li>解析 RTP 包（验证格式）</li>
     *   <li>查找转发会话</li>
     *   <li>复制 RTP 包并转发给所有订阅者</li>
     * </ol>
     *
     * <h2>性能优化</h2>
     * <ul>
     *   <li>使用 ByteBuf.retainedDuplicate() 避免数据复制</li>
     *   <li>快速地址映射查找（ConcurrentHashMap）</li>
     *   <li>批量转发减少系统调用</li>
     * </ul>
     *
     * @param ctx    Netty 上下文
     * @param packet UDP 数据包
     */
    private void handleRtp(ChannelHandlerContext ctx, DatagramPacket packet) {
        // 通过发送者地址查找连接 ID
        String connId = addressToConnectionId.get(packet.sender());
        if (connId == null) {
            // 未建立连接，忽略
            return;
        }

        ConnectionManager.ConnectionInfo conn = connectionManager.getConnection(connId);
        if (conn == null) {
            return;
        }

        // 仅处理推流者发送的 RTP（subscriberId="sfu-upstream" 表示推流者）
        if ("sfu-upstream".equals(conn.getSubscriberId())) {
            // 解析 RTP 包（验证格式）
            RtpPacket rtp = RtpPacket.decode(packet.content());
            if (rtp != null) {
                try {
                    // 查找转发会话
                    ForwardingSession session = mediaForwarder.getSession(conn.getStreamId());
                    if (session != null) {
                        // 记录转发统计
                        session.recordForwarding(packet.content().readableBytes());

                        // 转发给所有订阅者
                        for (SubscriberInfo sub : session.getSubscribers().values()) {
                            ConnectionManager.ConnectionInfo subConn = connectionManager.getConnection(
                                    conn.getPublisherId(), sub.getSubscriberId(), conn.getStreamId());

                            if (subConn != null && subConn.getRemoteAddress() != null) {
                                // 使用 retainedDuplicate 避免数据复制
                                // 注意：每个订阅者都会增加引用计数，需要确保正确释放
                                ByteBuf duplicate = packet.content().retainedDuplicate();
                                ctx.writeAndFlush(new DatagramPacket(duplicate, subConn.getRemoteAddress()));
                            }
                        }
                    }
                } finally {
                    // 释放 RTP 包资源
                    rtp.release();
                }
            }
        }
    }

    /**
     * 处理 RTCP 包
     * <p>
     * RTCP 用于质量反馈和控制。
     * 当前实现主要处理 PLI (Picture Loss Indication) 请求。
     *
     * <h2>支持的 RTCP 消息</h2>
     * <ul>
     *   <li><b>PLI (PT=206, FMT=1)</b>: 请求关键帧，当接收端检测到丢包时发送</li>
     *   <li><b>FIR (PT=206, FMT=4)</b>: 请求完整关键帧</li>
     *   <li><b>NACK (PT=205, FMT=1)</b>: 请求重传丢失的 RTP 包（待实现）</li>
     * </ul>
     *
     * <h2>PLI 转发流程</h2>
     * <ol>
     *   <li>订阅者检测到丢包，发送 PLI 请求</li>
     *   <li>服务器收到 PLI，转发给推流者</li>
     *   <li>推流者收到 PLI，生成关键帧（IDR 帧）</li>
     *   <li>推流者发送关键帧，订阅者恢复播放</li>
     * </ol>
     *
     * @param ctx    Netty 上下文
     * @param packet UDP 数据包
     */
    private void handleRtcp(ChannelHandlerContext ctx, DatagramPacket packet) {
        // 通过发送者地址查找连接 ID
        String connId = addressToConnectionId.get(packet.sender());
        if (connId == null) {
            return;
        }

        ConnectionManager.ConnectionInfo conn = connectionManager.getConnection(connId);
        if (conn == null) {
            return;
        }

        // 解析 RTCP 包
        RtcpPacket rtcp = RtcpPacket.decode(packet.content());
        if (rtcp != null) {
            try {
                // 处理 PLI (Picture Loss Indication) 请求
                if (rtcp.isPli()) {
                    // 查找转发会话
                    ForwardingSession session = mediaForwarder.getSession(conn.getStreamId());
                    if (session != null) {
                        // 查找推流者连接
                        ConnectionManager.ConnectionInfo pubConn = connectionManager.getConnection(
                                conn.getPublisherId(), "sfu-upstream", conn.getStreamId());

                        if (pubConn != null && pubConn.getRemoteAddress() != null) {
                            // 转发 PLI 请求给推流者
                            ByteBuf duplicate = packet.content().retainedDuplicate();
                            ctx.writeAndFlush(new DatagramPacket(duplicate, pubConn.getRemoteAddress()));

                            if (log.isDebugEnabled()) {
                                log.debug("转发 PLI 请求: streamId={}, subscriber={} -> publisher={}",
                                        conn.getStreamId(), conn.getSubscriberId(), conn.getPublisherId());
                            }
                        }
                    }
                }
                // TODO: 处理其他 RTCP 消息类型（FIR, NACK, SR, RR 等）
            } finally {
                rtcp.release();
            }
        }
    }

    /**
     * 清理地址映射
     * <p>
     * 当连接断开时，清理对应的地址映射。
     *
     * @param connectionId 连接 ID
     */
    public void removeAddressMapping(String connectionId) {
        addressToConnectionId.entrySet().removeIf(entry -> entry.getValue().equals(connectionId));
    }

    /**
     * 获取地址映射数量（用于统计）
     *
     * @return 映射数量
     */
    public int getAddressMappingCount() {
        return addressToConnectionId.size();
    }
}

