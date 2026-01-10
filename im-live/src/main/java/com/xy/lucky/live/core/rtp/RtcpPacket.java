package com.xy.lucky.live.core.rtp;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;

/**
 * RTCP (RTP Control Protocol) 数据包
 * <p>
 * 用于解析和封装 RTCP 控制包，遵循 RFC 3550 标准。
 * RTCP 用于提供 RTP 会话的质量反馈、同步信息等。
 *
 * <h2>RTCP 包类型</h2>
 * <ul>
 *   <li>200 (SR): Sender Report - 发送者报告</li>
 *   <li>201 (RR): Receiver Report - 接收者报告</li>
 *   <li>202 (SDES): Source Description - 源描述</li>
 *   <li>203 (BYE): Goodbye - 离开通知</li>
 *   <li>204 (APP): Application-defined - 应用定义</li>
 *   <li>205 (RTPFB): Transport layer feedback - 传输层反馈</li>
 *   <li>206 (PSFB): Payload-specific feedback - 负载特定反馈</li>
 * </ul>
 *
 * <h2>RTCP 包结构</h2>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|    RC   |   PT=SR=200   |             length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         SSRC of sender                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <h2>关键 RTCP 消息</h2>
 * <ul>
 *   <li><b>PLI (Picture Loss Indication)</b>: PT=206, FMT=1 - 请求关键帧</li>
 *   <li><b>FIR (Full Intra Request)</b>: PT=206, FMT=4 - 请求完整帧</li>
 *   <li><b>NACK (Negative Acknowledgement)</b>: PT=205, FMT=1 - 请求重传</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 * @see <a href="https://tools.ietf.org/html/rfc3550">RFC 3550 - RTCP</a>
 * @see <a href="https://tools.ietf.org/html/rfc4585">RFC 4585 - RTP/AVPF</a>
 */
@Data
@ToString(exclude = "payload")
public class RtcpPacket {

    /**
     * RTCP 包类型常量
     */
    public static final int PT_SR = 200;        // Sender Report
    public static final int PT_RR = 201;        // Receiver Report
    public static final int PT_SDES = 202;      // Source Description
    public static final int PT_BYE = 203;       // Goodbye
    public static final int PT_APP = 204;       // Application-defined
    public static final int PT_RTPFB = 205;     // Transport layer feedback
    public static final int PT_PSFB = 206;     // Payload-specific feedback
    /**
     * PSFB 反馈消息格式
     */
    public static final int FMT_PLI = 1;         // Picture Loss Indication
    public static final int FMT_SLI = 2;        // Slice Loss Indication
    public static final int FMT_RPSI = 3;       // Reference Picture Selection Indication
    public static final int FMT_FIR = 4;        // Full Intra Request
    public static final int FMT_TSTR = 5;       // Temporal-Spatial Trade-off Request
    public static final int FMT_TSTN = 6;       // Temporal-Spatial Trade-off Notification
    /**
     * RTCP 版本号，当前为 2
     */
    private static final int RTCP_VERSION = 2;
    /**
     * RTCP 固定头最小长度（8 字节）
     */
    private static final int RTCP_HEADER_MIN_SIZE = 8;
    /**
     * 版本号 (V): 2 bits
     */
    private int version;

    /**
     * 填充位 (P): 1 bit
     */
    private boolean padding;

    /**
     * 报告计数 (RC): 5 bits，表示报告块数量
     */
    private int reportCount;

    /**
     * 包类型 (PT): 8 bits
     */
    private int payloadType;

    /**
     * 长度: 16 bits，以 32-bit 字为单位（包含头部）
     */
    private int length;

    /**
     * SSRC: 32 bits，同步源标识符
     */
    private long ssrc;

    /**
     * 负载数据
     */
    private ByteBuf payload;

    /**
     * 原始数据包总大小（字节）
     */
    private int size;

    /**
     * 反馈消息格式（用于 PSFB/RTPFB）
     */
    private int feedbackMessageType;

    /**
     * 媒体源 SSRC（用于 PSFB/RTPFB）
     */
    private long mediaSourceSsrc;

    /**
     * 从 ByteBuf 解析 RTCP 包
     * <p>
     * 注意：返回的 payload 是原 ByteBuf 的 slice，需要自行管理引用计数
     *
     * @param buf 数据缓冲
     * @return RTCP 包，如果解析失败返回 null
     */
    public static RtcpPacket decode(ByteBuf buf) {
        if (buf == null || buf.readableBytes() < RTCP_HEADER_MIN_SIZE) {
            return null;
        }

        int startReaderIndex = buf.readerIndex();

        try {
            // Byte 0: V(2) + P(1) + RC(5)
            byte b0 = buf.readByte();
            int version = (b0 & 0xC0) >> 6;
            if (version != RTCP_VERSION) {
                return null;
            }
            boolean padding = (b0 & 0x20) != 0;
            int reportCount = b0 & 0x1F;

            // Byte 1: PT
            int payloadType = buf.readUnsignedByte();

            // Bytes 2-3: Length (以 32-bit 字为单位)
            int length = buf.readUnsignedShort();

            // Bytes 4-7: SSRC
            long ssrc = buf.readUnsignedInt();

            // 计算实际长度（字节）
            int actualLength = (length + 1) * 4; // +1 是因为长度字段不包含头部本身
            int remainingBytes = buf.readableBytes();

            if (remainingBytes < actualLength - 8) {
                // 数据不完整
                return null;
            }

            // 解析特定类型的额外字段
            int feedbackMessageType = 0;
            long mediaSourceSsrc = 0;

            if (payloadType == PT_PSFB || payloadType == PT_RTPFB) {
                // 反馈消息：FMT 在 RC 字段中，SSRC 后是媒体源 SSRC
                feedbackMessageType = reportCount;
                if (remainingBytes >= 4) {
                    mediaSourceSsrc = buf.readUnsignedInt();
                }
            }

            // Payload（剩余数据）
            int payloadLength = actualLength - (buf.readerIndex() - startReaderIndex);
            ByteBuf payload = payloadLength > 0 ? buf.readSlice(payloadLength).retain() : null;

            RtcpPacket packet = new RtcpPacket();
            packet.setVersion(version);
            packet.setPadding(padding);
            packet.setReportCount(reportCount);
            packet.setPayloadType(payloadType);
            packet.setLength(length);
            packet.setSsrc(ssrc);
            packet.setFeedbackMessageType(feedbackMessageType);
            packet.setMediaSourceSsrc(mediaSourceSsrc);
            packet.setPayload(payload);
            packet.setSize(actualLength);

            return packet;
        } catch (Exception e) {
            // 解析失败，恢复读取位置
            buf.readerIndex(startReaderIndex);
            return null;
        }
    }

    /**
     * 创建 PLI 请求包
     * <p>
     * 用于请求关键帧
     *
     * @param senderSsrc 发送者 SSRC
     * @param mediaSsrc  媒体源 SSRC
     * @return RTCP 包
     */
    public static RtcpPacket createPli(long senderSsrc, long mediaSsrc) {
        RtcpPacket packet = new RtcpPacket();
        packet.setVersion(RTCP_VERSION);
        packet.setPadding(false);
        packet.setReportCount(FMT_PLI); // FMT=1
        packet.setPayloadType(PT_PSFB);
        packet.setSsrc(senderSsrc);
        packet.setMediaSourceSsrc(mediaSsrc);
        packet.setFeedbackMessageType(FMT_PLI);
        // Length = (header + media SSRC) / 4 - 1 = (8 + 4) / 4 - 1 = 2
        packet.setLength(2);
        return packet;
    }

    /**
     * 检查是否是 PLI (Picture Loss Indication) 请求
     * <p>
     * PLI 用于请求关键帧，当接收端检测到丢包时发送
     *
     * @return true 如果是 PLI 请求
     */
    public boolean isPli() {
        return payloadType == PT_PSFB && feedbackMessageType == FMT_PLI;
    }

    /**
     * 检查是否是 FIR (Full Intra Request) 请求
     * <p>
     * FIR 用于请求完整的关键帧
     *
     * @return true 如果是 FIR 请求
     */
    public boolean isFir() {
        return payloadType == PT_PSFB && feedbackMessageType == FMT_FIR;
    }

    /**
     * 检查是否是 NACK (Negative Acknowledgement) 请求
     * <p>
     * NACK 用于请求重传丢失的 RTP 包
     *
     * @return true 如果是 NACK 请求
     */
    public boolean isNack() {
        return payloadType == PT_RTPFB && feedbackMessageType == 1;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (payload != null && payload.refCnt() > 0) {
            payload.release();
        }
    }
}

