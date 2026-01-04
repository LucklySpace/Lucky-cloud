package com.xy.lucky.live.rtmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RTMP 命令处理（极简）
 * 仅识别：
 * - connect
 * - createStream
 * - publish
 * - play
 */
public class RtmpCommandHandler extends ChannelInboundHandlerAdapter {
    public static final AttributeKey<String> ATTR_APP = AttributeKey.valueOf("rtmp.app");
    public static final AttributeKey<String> ATTR_STREAM = AttributeKey.valueOf("rtmp.stream");
    private static final Logger log = LoggerFactory.getLogger(RtmpCommandHandler.class);
    private final StreamRegistry registry;
    private double txnId = 1.0;
    private int streamId = 1;
    private String appName;
    private String streamName;

    public RtmpCommandHandler(StreamRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            ctx.fireChannelRead(msg);
            return;
        }
        ByteBuf in = (ByteBuf) msg;
        try {
            Object nameObj = Amf0.read(in);
            if (!(nameObj instanceof String cmd)) {
                return;
            }
            Double txn = (Double) Amf0.read(in);
            Object third = Amf0.read(in);
            switch (cmd) {
                case "connect" -> {
                    appName = extractApp(third);
                    ctx.channel().attr(ATTR_APP).set(appName);
                    writeResult(ctx, "_result", txn != null ? txn : ++txnId);
                }
                case "createStream" -> {
                    writeResultWithNumber(ctx, "_result", txn != null ? txn : ++txnId, ++streamId);
                }
                case "publish" -> {
                    streamName = readString(in);
                    ctx.channel().attr(ATTR_STREAM).set(streamName);
                    registry.publish(appName, streamName, ctx.channel());
                    log.info("开始推流: app={}, stream={}", appName, streamName);
                }
                case "play" -> {
                    streamName = readString(in);
                    ctx.channel().attr(ATTR_STREAM).set(streamName);
                    registry.play(appName, streamName, ctx.channel());
                    log.info("开始拉流: app={}, stream={}", appName, streamName);
                }
                default -> log.debug("忽略命令: {}", cmd);
            }
        } catch (Exception e) {
            log.warn("RTMP 命令处理异常", e);
        } finally {
            if (in.refCnt() > 0) in.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("RTMP 命令处理异常", cause);
        ctx.close();
    }

    private String extractApp(Object third) {
        if (third instanceof Map<?, ?> map) {
            Object app = map.get("app");
            if (app instanceof String s) return s;
        }
        return "live";
    }

    private String readString(ByteBuf in) {
        int len = in.readUnsignedShort();
        byte[] b = new byte[len];
        in.readBytes(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    private void writeResult(ChannelHandlerContext ctx, String name, double txn) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(0x02);
        byte[] n = name.getBytes(StandardCharsets.UTF_8);
        out.writeShort(n.length);
        out.writeBytes(n);
        out.writeByte(0x00);
        out.writeLong(Double.doubleToLongBits(txn));
        out.writeByte(0x03);
        out.writeShort(0);
        out.writeByte(0x09);
        ctx.writeAndFlush(out);
    }

    private void writeResultWithNumber(ChannelHandlerContext ctx, String name, double txn, int num) {
        ByteBuf out = Unpooled.buffer();
        out.writeByte(0x02);
        byte[] n = name.getBytes(StandardCharsets.UTF_8);
        out.writeShort(n.length);
        out.writeBytes(n);
        out.writeByte(0x00);
        out.writeLong(Double.doubleToLongBits(txn));
        out.writeByte(0x05);
        out.writeByte(0x00);
        out.writeLong(Double.doubleToLongBits(num));
        ctx.writeAndFlush(out);
    }
}

