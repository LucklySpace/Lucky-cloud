package com.xy.connect.netty;


import com.xy.connect.config.LogConstant;
import com.xy.imcore.utils.JwtUtil;
import com.xy.imcore.utils.StringUtils;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.xy.imcore.constants.IMConstant.IM_USER;


/**
 * 认证成功则在通道添加聊天处理的 handler, 且需要修改 websocket 连接的 uri, 交由新引入的 handler 处理
 * 对 WebSocket 握手连接请求进行拦截与鉴权：
 * - 校验 token 合法性
 * - 将 userId 存入 Channel 属性
 * - 修改 URI 并注入必要的处理器（如心跳检测、业务处理器）
 * - 鉴权失败则关闭连接
 * https://blog.csdn.net/qq_40264499/article/details/126070215#:~:text=Netty%E6%98%AF
 */
@Slf4j(topic = LogConstant.Netty)
@Component
@ChannelHandler.Sharable
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * 用户 ID 存入 Channel 的 AttributeKey
     */
    public static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf(IM_USER);

    /**
     * 心跳检测时间间隔（单位：毫秒），通过配置文件注入
     */
    @Value("netty.config.heartBeatTime")
    private Integer heartBeatTime;

    /**
     * 主业务处理器
     */
    @Autowired
    private IMChannelHandler imChannelHandler;

    /**
     * 处理 WebSocket 握手请求前的 token 鉴权逻辑
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        String uri = request.uri();

        log.info("拦截到 WebSocket 握手请求: {}", uri);

        try {
            // 1. 提取 token
            String token = extractTokenFromUri(uri);
            if (token == null) {
                throw new IllegalArgumentException("请求缺少 token 参数");
            }

            // 2. 校验 token 合法性
            if (!JwtUtil.validate(token)) {
                throw new IllegalArgumentException("token 无效");
            }

            // 3. 获取用户 ID 并绑定到 Channel 属性
            String userId = JwtUtil.getUsername(token);
            if (StringUtils.isBlank(userId)) {

                throw new IllegalArgumentException("token 无法解析用户信息");

            }
            ctx.channel().attr(USER_ID_ATTR_KEY).set(userId);

            // 4. 修改 URI，避免 `/ws?token=xxx` 干扰 WebSocket 协议升级
            request.setUri("/im");

            // 5. 添加 Netty 心跳检测器（基于 ALL_IDLE）
            ctx.pipeline().addLast(new IdleStateHandler(0, 0, heartBeatTime, TimeUnit.MILLISECONDS));

            // 6. 添加业务处理器
            ctx.pipeline().addLast(imChannelHandler);

            // 7. 继续传递请求（保留请求引用）
            ctx.fireChannelRead(request.retain());

        } catch (Exception e) {

            log.error("WebSocket 鉴权失败，原因: {}", e.getMessage(), e);

            sendUnauthorizedResponse(ctx);
        }
    }

    /**
     * 从 URI 中解析 token 参数
     */
    private String extractTokenFromUri(String uri) {
        try {
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            List<String> tokens = decoder.parameters().get("token");
            return (tokens != null && !tokens.isEmpty()) ? tokens.get(0) : null;
        } catch (Exception e) {
            log.warn("URI 解析失败: {}", uri, e);
            return null;
        }
    }

    /**
     * 向客户端发送 401 响应并关闭连接
     */
    private void sendUnauthorizedResponse(ChannelHandlerContext ctx) {
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
