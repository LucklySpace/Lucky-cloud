package com.xy.connect.netty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xy.connect.domain.proto.ImConnectProto;
import com.xy.core.constants.IMConstant;
import com.xy.core.model.IMConnectMessage;
import com.xy.core.utils.JwtUtil;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * AuthHandler - 支持 WebSocket 握手鉴权 & TCP 长连接鉴权
 * <p>
 * 优化要点：
 * - 合并公共 token 验证逻辑，提高复用。
 * - 简化提取 token 的 try-catch，减少异常开销。
 * - 优化 ByteBuf 处理：使用 retainedDuplicate 避免多次 copy，高效解析。
 * - 修复 Cookie 解析逻辑。
 * - 移除不必要日志和操作，精简 pipeline 注入检查。
 * - 确保引用计数严格管理，防止泄漏。
 * - 恢复关键日志输出以便调试，同时避免性能敏感路径过多日志。
 */
@Slf4j(topic = "Auth")
@Component
@ChannelHandler.Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<String> USER_ID_ATTR_KEY = AttributeKey.valueOf(IMConstant.IM_USER);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${netty.config.heartBeatTime}")
    private Integer heartBeatTime;

    @Value("${netty.config.protocol}")
    private String protocolType;

    @Autowired
    private IMLoginHandler imLoginHandler;

    @Autowired
    private IMHeartBeatHandler imHeartBeatHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof FullHttpRequest) {
                handleHttpHandshake(ctx, (FullHttpRequest) msg);
                return;
            }
            if (msg instanceof ByteBuf) {
                handleTcpInitialByteBuf(ctx, (ByteBuf) msg);
                return;
            }
            if (msg instanceof String) {
                handleTcpInitialString(ctx, (String) msg);
                return;
            }
            if (msg instanceof IMConnectMessage) {
                handleTcpPojo(ctx, (IMConnectMessage<?>) msg);
                return;
            }
            ctx.fireChannelRead(msg);
        } catch (Exception ex) {
            log.error("AuthHandler exception: {}", ex.getMessage(), ex);
            ReferenceCountUtil.release(msg);
            closeOnFailure(ctx);
        }
    }

    /**
     * 认证通过添加处理器
     */
    private void ensurePostAuthPipeline(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        if (p.get(IMLoginHandler.class) == null) {
            p.addLast("idle", new IdleStateHandler(0, 0, heartBeatTime, TimeUnit.MILLISECONDS));
            p.addLast("heartBeat", imHeartBeatHandler);
            p.addLast("login", imLoginHandler);
            log.info("已为 channel 注入 IdleStateHandler 与 IMChannelHandler");
        }
        try {
            p.remove(this);
            log.debug("AuthHandler 已从 pipeline 中移除");
        } catch (NoSuchElementException ignored) {
        }
    }


    private void handleHttpHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {
        log.info("拦截到 HTTP 握手请求: {}", request.uri());
        String token = extractTokenFromHttpRequest(request);
        String userId = validateAndGetUserId(token);
        if (userId == null) {
            log.warn("HTTP 握手 token 无效");
            sendHttpUnauthorizedAndClose(ctx);
            ReferenceCountUtil.release(request);
            return;
        }
        log.info("HTTP 握手鉴权成功 userId={}", userId);
        request.setUri("/im");
        ctx.channel().attr(USER_ID_ATTR_KEY).set(userId);
        ensurePostAuthPipeline(ctx);
        ctx.fireChannelRead(request.retain());
    }

    private void handleTcpInitialByteBuf(ChannelHandlerContext ctx, ByteBuf buf) {
        ByteBuf duplicate = buf.retainedDuplicate(); // 高效 duplicate，避免 copy
        try {
            byte[] bytes = new byte[duplicate.readableBytes()];
            duplicate.readBytes(bytes); // 现在可以破坏 duplicate
            String token = "proto".equalsIgnoreCase(protocolType) ?
                    tryExtractTokenFromProtoBytes(bytes) != null ? tryExtractTokenFromProtoBytes(bytes) : tryExtractTokenFromJsonBytes(bytes) :
                    tryExtractTokenFromJsonBytes(bytes) != null ? tryExtractTokenFromJsonBytes(bytes) : tryExtractTokenFromProtoBytes(bytes);

            String userId = validateAndGetUserId(token);
            if (userId == null) {
                log.warn("TCP ByteBuf token 无效");
                closeOnFailure(ctx);
                return;
            }
            log.info("TCP ByteBuf 鉴权成功 userId={}", userId);
            ctx.channel().attr(USER_ID_ATTR_KEY).set(userId);
            ensurePostAuthPipeline(ctx);
            ctx.fireChannelRead(buf.retain());
        } finally {
            duplicate.release();
        }
    }

    private void handleTcpInitialString(ChannelHandlerContext ctx, String txt) {
        String token = tryExtractTokenFromString(txt);
        String userId = validateAndGetUserId(token);
        if (userId == null) {
            log.warn("TCP String token 无效");
            closeOnFailure(ctx);
            return;
        }
        log.info("TCP String 鉴权成功 userId={}", userId);
        ctx.channel().attr(USER_ID_ATTR_KEY).set(userId);
        ensurePostAuthPipeline(ctx);
        ctx.fireChannelRead(txt);
    }

    private void handleTcpPojo(ChannelHandlerContext ctx, IMConnectMessage<?> pojo) {
        String token = pojo.getToken();
        String userId = validateAndGetUserId(token);
        if (userId == null) {
            log.warn("TCP POJO token 无效");
            closeOnFailure(ctx);
            return;
        }
        log.info("TCP POJO 鉴权成功 userId={}", userId);
        ctx.channel().attr(USER_ID_ATTR_KEY).set(userId);
        ensurePostAuthPipeline(ctx);
        ctx.fireChannelRead(pojo);
    }

    // 公共验证方法：简化重复代码
    private String validateAndGetUserId(String token) {
        if (StringUtils.isBlank(token) || !JwtUtil.validate(token)) {
            return null;
        }
        String userId = JwtUtil.getUsername(token);
        return StringUtils.isBlank(userId) ? null : userId;
    }

    private String extractTokenFromHttpRequest(FullHttpRequest req) {
        // Query param
        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        List<String> tokens = decoder.parameters().get("token");
        if (tokens != null && !tokens.isEmpty()) {
            return tokens.getFirst();
        }

        // Authorization header
        String auth = req.headers().get("Authorization");
        if (StringUtils.isNotBlank(auth)) {
            auth = auth.trim();
            if (auth.toLowerCase().startsWith("bearer ")) {
                return auth.substring(7).trim();
            }
            return auth;
        }

        // Cookie
        String cookieHeader = req.headers().get("Cookie");
        if (StringUtils.isNotBlank(cookieHeader)) {
            Cookie cookie = ClientCookieDecoder.LAX.decode(cookieHeader);
            if (cookie != null && "token".equalsIgnoreCase(cookie.name())) {
                return cookie.value();
            }
        }
        return null;
    }

    private void sendHttpUnauthorizedAndClose(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    private String tryExtractTokenFromProtoBytes(byte[] bytes) {
        try {
            ImConnectProto.IMConnectMessage proto = ImConnectProto.IMConnectMessage.parseFrom(bytes);
            return StringUtils.isNotBlank(proto.getToken()) ? proto.getToken() : null;
        } catch (InvalidProtocolBufferException ignored) {
            return null;
        }
    }

    private String tryExtractTokenFromJsonBytes(byte[] bytes) {
        try {
            String txt = new String(bytes, StandardCharsets.UTF_8).trim();
            if (txt.isEmpty()) return null;
            JsonNode node = MAPPER.readTree(txt);
            JsonNode t = node.get("token");
            if (t != null && t.isTextual()) return t.asText();
            JsonNode data = node.get("data");
            if (data != null) {
                t = data.get("token");
                if (t != null && t.isTextual()) return t.asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String tryExtractTokenFromString(String s) {
        if (StringUtils.isBlank(s)) return null;
        String t = s.trim();
        int idx = t.indexOf("token=");
        if (idx >= 0) {
            String sub = t.substring(idx + 6);
            int amp = sub.indexOf('&');
            return (amp >= 0 ? sub.substring(0, amp) : sub).trim();
        }
        try {
            JsonNode n = MAPPER.readTree(t);
            JsonNode tokenNode = n.get("token");
            if (tokenNode != null && tokenNode.isTextual()) return tokenNode.asText();
        } catch (Exception ignored) {
        }
        return null;
    }

    private void closeOnFailure(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("AuthHandler exceptionCaught", cause);
        closeOnFailure(ctx);
    }
}