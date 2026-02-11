package com.xy.lucky.connect.netty.service.tcp.codec.json;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.model.IMessageWrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * TCP JSON 消息编解码处理器（双工）
 * <p>
 * - 读取时：将 ByteBuf 解码为 IMessageWrap POJO
 * - 写入时：将 IMessageWrap POJO 编码为 ByteBuf
 * <p>
 * 注意：前置 pipeline 应添加 LengthFieldBasedFrameDecoder 和 LengthFieldPrepender 进行帧处理
 */
@Slf4j(topic = LogConstant.Netty)
@ChannelHandler.Sharable
public class TcpJsonMessageHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf buf) {
            try {
                // 读取所有可读字节
                int len = buf.readableBytes();
                byte[] bytes = new byte[len];
                buf.readBytes(bytes);
                String text = new String(bytes, StandardCharsets.UTF_8);

                // 解析 JSON 为 IMessageWrap
                IMessageWrap<?> pojo = JacksonUtil.parseObject(text, IMessageWrap.class);
                if (pojo != null) {
                    ctx.fireChannelRead(pojo);
                } else {
                    log.warn("TCP JSON 解码结果为 null, text={}", truncate(text, 256));
                }
            } catch (Exception e) {
                log.error("TCP JSON 解码失败: {}", e.getMessage());
                // 解析失败，可选择关闭连接或丢弃
            } finally {
                // 释放 ByteBuf
                buf.release();
            }
        } else if (msg instanceof IMessageWrap<?> pojo) {
            // 已经是 POJO，直接传递
            ctx.fireChannelRead(pojo);
        } else {
            // 未知类型，传递给下一个 Handler
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof IMessageWrap<?> imMsg) {
            try {
                String json = JacksonUtil.toJSONString(imMsg);
                if (json != null) {
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    ByteBuf buf = Unpooled.wrappedBuffer(bytes);
                    ctx.write(buf, promise);
                } else {
                    log.warn("TCP JSON 编码结果为 null");
                    ctx.write(msg, promise);
                }
            } catch (Exception e) {
                log.error("TCP JSON 编码失败: {}", e.getMessage());
                ctx.write(msg, promise);
            }
        } else {
            // 非 IMessageWrap，直接传递
            ctx.write(msg, promise);
        }
    }

    /**
     * 截断字符串（避免日志过长）
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "<null>";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(truncated)";
    }
}

