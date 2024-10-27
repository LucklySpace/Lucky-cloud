package com.xy.connect.netty;

import com.xy.connect.config.ConfigCenter;
import com.xy.connect.config.LogConstant;
import com.xy.imcore.utils.JwtUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import static com.xy.imcore.constants.Constant.IMUSER;


/**
 * 对连接请求进行拦截：
 * 认证成功则在通道添加聊天处理的 handler, 且需要修改 websocket 连接的 uri, 交由新引入的 handler 处理
 * 认证失败则关闭连接
 * https://blog.csdn.net/qq_40264499/article/details/126070215#:~:text=Netty%E6%98%AF
 */
@Slf4j(topic = LogConstant.NETTY)
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    public static AttributeKey key = AttributeKey.valueOf(IMUSER);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        log.info("认证拦截：[{}]", request.uri());
        try {
            String uri = request.uri();

            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            // 获取 token
            String token = decoder.parameters().get("token").get(0);
            // 校验token
            if (!validateToken(token)) {
                throw new RuntimeException("token无效");
            }
            // 修改 uri
            request.setUri("/im");

            // 将 user_id 存入 channel 属性中
            ctx.channel().attr(key).set(JwtUtil.getUsername(token));

            ctx.fireChannelRead(request.retain());
            // 添加心跳检测
            ctx.pipeline().addLast(new IdleStateHandler(0, 0, ConfigCenter.nettyConfig.getNettyConfig().getHeartBeatTime(), TimeUnit.MILLISECONDS));
            // 添加 消息处理器
            ctx.pipeline().addLast(new IMChannelHandler());

        } catch (Exception e) {
            log.error("WS认证拦截异常：[{}]", e.getMessage());
            ctx.close();
        }
    }

    /**
     * 验证 token 是否有效
     *
     * @return token 是否有效
     */
    private boolean validateToken(String token) {
        return token != null && JwtUtil.validate(token);
    }

}

