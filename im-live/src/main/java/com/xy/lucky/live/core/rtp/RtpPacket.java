package com.xy.lucky.live.core.rtp;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.ToString;

/**
 * RTP (Real-time Transport Protocol) 数据包
 * <p>
 * 用于解析和封装 RTP 数据包，遵循 RFC 3550 标准。
 * RTP 是 WebRTC 中用于传输音视频数据的核心协议。
 *
 * <h2>RTP 包结构</h2>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           timestamp                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           synchronization source (SSRC) identifier            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |            contributing source (CSRC) identifiers             |
 * |                             ....                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                    RTP extension header                       |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         payload data                          |
 * |                             ....                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <h2>字段说明</h2>
 * <ul>
 *   <li>V (2 bits): 版本号，当前为 2</li>
 *   <li>P (1 bit): 填充位，数据包末尾包含填充字节</li>
 *   <li>X (1 bit): 扩展位，固定头后紧跟扩展头</li>
 *   <li>CC (4 bits): CSRC 计数，表示 CSRC 标识符个数</li>
 *   <li>M (1 bit): 标记位，视频中通常表示一帧结束</li>
 *   <li>PT (7 bits): 负载类型，表示媒体格式（如 H.264=96, VP8=97）</li>
 *   <li>sequence number (16 bits): 序列号，每发送一个包递增</li>
 *   <li>timestamp (32 bits): 时间戳，反映采样时刻</li>
 *   <li>SSRC (32 bits): 同步源标识符，唯一标识同步源</li>
 * </ul>
 *
 * @author lucky
 * @version 1.0.0
 * @see <a href="https://tools.ietf.org/html/rfc3550">RFC 3550 - RTP</a>
 */
@Data
@ToString(exclude = "payload")
public class RtpPacket {

    /**
     * RTP 版本号，当前为 2
     */
    private static final int RTP_VERSION = 2;

    /**
     * RTP 固定头最小长度（12 字节）
     */
    private static final int RTP_HEADER_MIN_SIZE = 12;

    /**
     * 版本号 (V): 2 bits，当前版本为 2
     */
    private int version;

    /**
     * 填充位 (P): 1 bit，如果为 1，则数据包末尾包含填充字节
     */
    private boolean padding;

    /**
     * 扩展位 (X): 1 bit，如果为 1，则固定头后面紧跟扩展头
     */
    private boolean extension;

    /**
     * CSRC 计数 (CC): 4 bits，表示 CSRC 标识符的个数
     */
    private int csrcCount;

    /**
     * 标记位 (M): 1 bit，对于视频，通常表示一帧的结束
     */
    private boolean marker;

    /**
     * 负载类型 (PT): 7 bits，表示媒体负载的格式
     * <p>
     * 常见值：
     * <ul>
     *   <li>96: H.264 视频</li>
     *   <li>97: VP8 视频</li>
     *   <li>98: VP9 视频</li>
     *   <li>111: Opus 音频</li>
     *   <li>103: ISAC 音频</li>
     * </ul>
     */
    private int payloadType;

    /**
     * 序列号: 16 bits，每发送一个 RTP 包增加 1
     * 用于检测丢包和乱序
     */
    private int sequenceNumber;

    /**
     * 时间戳: 32 bits，反映采样时刻
     * 用于同步和播放控制
     */
    private long timestamp;

    /**
     * 同步源标识符 (SSRC): 32 bits，唯一标识同步源
     * 用于区分不同的媒体流
     */
    private long ssrc;

    /**
     * 贡献源列表 (CSRC)
     * 在混音场景中，标识参与混音的源
     */
    private long[] csrcs;

    /**
     * 扩展头数据（如果存在）
     */
    private ByteBuf extensionHeader;

    /**
     * 负载数据
     * 包含实际的音视频编码数据
     */
    private ByteBuf payload;

    /**
     * 原始数据包总大小（字节）
     */
    private int size;

    /**
     * 从 ByteBuf 解析 RTP 包
     * <p>
     * 注意：返回的 payload 是原 ByteBuf 的 slice，需要自行管理引用计数
     *
     * @param buf 数据缓冲
     * @return RTP 包，如果解析失败返回 null
     */
    public static RtpPacket decode(ByteBuf buf) {
        if (buf == null || buf.readableBytes() < RTP_HEADER_MIN_SIZE) {
            return null;
        }

        int startReaderIndex = buf.readerIndex();

        try {
            // Byte 0: V(2) + P(1) + X(1) + CC(4)
            byte b0 = buf.readByte();
            int version = (b0 & 0xC0) >> 6;
            if (version != RTP_VERSION) {
                // 只支持 RTP v2
                return null;
            }
            boolean padding = (b0 & 0x20) != 0;
            boolean extension = (b0 & 0x10) != 0;
            int csrcCount = b0 & 0x0F;

            // Byte 1: M(1) + PT(7)
            byte b1 = buf.readByte();
            boolean marker = (b1 & 0x80) != 0;
            int payloadType = b1 & 0x7F;

            // Bytes 2-3: Sequence Number
            int sequenceNumber = buf.readUnsignedShort();

            // Bytes 4-7: Timestamp
            long timestamp = buf.readUnsignedInt();

            // Bytes 8-11: SSRC
            long ssrc = buf.readUnsignedInt();

            // CSRC list (每个 4 字节)
            long[] csrcs = new long[csrcCount];
            if (csrcCount > 0) {
                if (buf.readableBytes() < csrcCount * 4) {
                    return null;
                }
                for (int i = 0; i < csrcCount; i++) {
                    csrcs[i] = buf.readUnsignedInt();
                }
            }

            // Extension Header (如果存在)
            ByteBuf extensionHeader = null;
            if (extension) {
                if (buf.readableBytes() < 4) {
                    return null;
                }
                int profile = buf.readUnsignedShort();
                int length = buf.readUnsignedShort(); // 长度以 32-bit (4字节) 为单位
                if (buf.readableBytes() < length * 4) {
                    return null;
                }
                // 保存扩展头数据
                extensionHeader = buf.readSlice(length * 4).retain();
            }

            // Payload
            int payloadLength = buf.readableBytes();
            if (padding && payloadLength > 0) {
                // 获取最后一个字节作为填充长度
                int paddingLength = buf.getUnsignedByte(buf.readerIndex() + payloadLength - 1);
                if (paddingLength > 0 && paddingLength <= payloadLength) {
                    payloadLength -= paddingLength;
                }
            }

            if (payloadLength < 0) {
                return null;
            }

            ByteBuf payload = payloadLength > 0 ? buf.readSlice(payloadLength).retain() : null;

            RtpPacket packet = new RtpPacket();
            packet.setVersion(version);
            packet.setPadding(padding);
            packet.setExtension(extension);
            packet.setCsrcCount(csrcCount);
            packet.setMarker(marker);
            packet.setPayloadType(payloadType);
            packet.setSequenceNumber(sequenceNumber);
            packet.setTimestamp(timestamp);
            packet.setSsrc(ssrc);
            packet.setCsrcs(csrcs);
            packet.setExtensionHeader(extensionHeader);
            packet.setPayload(payload);
            packet.setSize(buf.readerIndex() - startReaderIndex);

            return packet;
        } catch (Exception e) {
            // 解析失败，恢复读取位置
            buf.readerIndex(startReaderIndex);
            return null;
        }
    }

    /**
     * 编码 RTP 包到 ByteBuf
     * <p>
     * 注意：返回的 ByteBuf 需要调用方负责释放
     *
     * @param buf 目标缓冲区（如果为 null 则创建新的）
     * @return 编码后的 ByteBuf
     */
    public ByteBuf encode(ByteBuf buf) {
        if (buf == null) {
            buf = io.netty.buffer.Unpooled.buffer(1500); // 典型 MTU 大小
        }

        // Byte 0
        byte b0 = (byte) ((version << 6) | (padding ? 0x20 : 0) | (extension ? 0x10 : 0) | (csrcCount & 0x0F));
        buf.writeByte(b0);

        // Byte 1
        byte b1 = (byte) ((marker ? 0x80 : 0) | (payloadType & 0x7F));
        buf.writeByte(b1);

        // Sequence Number
        buf.writeShort(sequenceNumber);

        // Timestamp
        buf.writeInt((int) timestamp);

        // SSRC
        buf.writeInt((int) ssrc);

        // CSRC list
        if (csrcs != null) {
            for (long csrc : csrcs) {
                buf.writeInt((int) csrc);
            }
        }

        // Extension Header
        if (extension && extensionHeader != null) {
            buf.writeBytes(extensionHeader);
        }

        // Payload
        if (payload != null) {
            buf.writeBytes(payload);
        }

        // Padding (如果需要)
        if (padding) {
            int paddingLength = 4 - (buf.writerIndex() % 4);
            if (paddingLength == 4) {
                paddingLength = 0;
            }
            if (paddingLength > 0) {
                buf.writeZero(paddingLength);
                buf.writeByte(paddingLength);
            }
        }

        return buf;
    }

    /**
     * 释放资源
     * <p>
     * 释放 payload 和 extensionHeader 的引用计数
     */
    public void release() {
        if (payload != null && payload.refCnt() > 0) {
            payload.release();
        }
        if (extensionHeader != null && extensionHeader.refCnt() > 0) {
            extensionHeader.release();
        }
    }

    /**
     * 检查是否是视频包
     *
     * @return true 如果是视频负载类型
     */
    public boolean isVideo() {
        // 常见的视频负载类型：96-127 通常是动态分配的
        // 这里简单判断：96-127 通常是视频，但实际应该根据 SDP 确定
        return payloadType >= 96 && payloadType <= 127;
    }

    /**
     * 检查是否是音频包
     *
     * @return true 如果是音频负载类型
     */
    public boolean isAudio() {
        // 常见的音频负载类型：0-95 是静态分配的
        // 111=Opus, 103=ISAC 等
        return payloadType < 96 || payloadType == 111 || payloadType == 103;
    }
}

