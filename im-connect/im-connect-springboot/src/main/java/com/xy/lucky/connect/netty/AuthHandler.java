package com.xy.lucky.connect.netty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.domain.proto.IMessageProto;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.core.utils.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * 高性能鉴权处理器
 * <p>
 * - 支持 WebSocket 握手鉴权 & TCP 长连接鉴权
 * - 零拷贝解析：避免不必要的 byte[] 分配
 * - 协议自适应：根据配置优先尝试对应协议解析
 * - 快速失败：鉴权失败立即关闭连接，减少资源占用
 * - 单次认证：认证通过后从 Pipeline 移除，不再参与后续消息处理
 */
@Slf4j(topic = LogConstant.Auth)
@Component
@ChannelHandler.Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    public static final AttributeKey<String> USER_ID_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    public static final AttributeKey<String> DEVICE_TYPE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BEARER_PREFIX = "bearer ";

    @Value("${netty.config.heartBeatTime}")
    private Integer heartBeatTime;

    @Value("${netty.config.protocol:json}")
    private String protocolType;

    @Autowired
    private LoginHandler loginHandler;

    @Autowired
    private HeartBeatHandler heartBeatHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof FullHttpRequest request) {
                handleHttpHandshake(ctx, request);
            } else if (msg instanceof ByteBuf buf) {
                handleByteBuf(ctx, buf);
            } else if (msg instanceof String text) {
                handleString(ctx, text);
            } else if (msg instanceof IMessageWrap<?> pojo) {
                handlePojo(ctx, pojo);
            } else {
                // 未知消息类型，传递给下一个 Handler
                ctx.fireChannelRead(msg);
            }
        } catch (Exception ex) {
            log.error("鉴权处理异常: channelId={}, error={}", ctx.channel().id().asShortText(), ex.getMessage());
            ReferenceCountUtil.release(msg);
            closeOnFailure(ctx);
        }
    }

    /**
     * 处理 HTTP 握手请求 (WebSocket 升级)
     */
    private void handleHttpHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {
        AuthResult result = extractFromHttpRequest(request);

        if (!result.isValid()) {
            log.warn("HTTP 握手鉴权失败: uri={}", request.uri());
            sendUnauthorizedAndClose(ctx);
            ReferenceCountUtil.release(request);
            return;
        }

        log.info("HTTP 握手鉴权成功: uri={}, userId={}, device={}", request.uri(), result.userId, result.deviceType);

        // 重写 URI 为标准 WebSocket 路径
        request.setUri("/im");

        // 绑定用户信息到 Channel
        bindUserAttributes(ctx, result);

        // 注入后续处理器并移除自身
        ensurePostAuthPipeline(ctx);

        // 保留引用并传递给下一个 Handler
        ctx.fireChannelRead(request.retain());
    }

    /**
     * 处理 TCP ByteBuf 消息
     */
    private void handleByteBuf(ChannelHandlerContext ctx, ByteBuf buf) {
        // 标记读索引，便于后续传递完整数据
        buf.markReaderIndex();

        AuthResult result = extractFromByteBuf(buf);

        // 重置读索引，确保下游 Handler 能读取完整数据
        buf.resetReaderIndex();

        if (!result.isValid()) {
            log.warn("TCP ByteBuf 鉴权失败");
            closeOnFailure(ctx);
            return;
        }

        log.info("TCP ByteBuf 鉴权成功: userId={}, device={}", result.userId, result.deviceType);

        bindUserAttributes(ctx, result);
        ensurePostAuthPipeline(ctx);
        ctx.fireChannelRead(buf.retain());
    }

    /**
     * 处理 TCP 文本消息
     */
    private void handleString(ChannelHandlerContext ctx, String text) {
        AuthResult result = extractFromString(text);

        if (!result.isValid()) {
            log.warn("TCP String 鉴权失败");
            closeOnFailure(ctx);
            return;
        }

        log.info("TCP String 鉴权成功: userId={}, device={}", result.userId, result.deviceType);

        bindUserAttributes(ctx, result);
        ensurePostAuthPipeline(ctx);
        ctx.fireChannelRead(text);
    }

    /**
     * 处理已解码的 POJO 消息
     */
    private void handlePojo(ChannelHandlerContext ctx, IMessageWrap<?> pojo) {
        String userId = validateToken(pojo.getToken());

        if (userId == null) {
            log.warn("TCP POJO 鉴权失败");
            closeOnFailure(ctx);
            return;
        }

        log.info("TCP POJO 鉴权成功: userId={}, device={}", userId, pojo.getDeviceType());

        bindUserAttributes(ctx, new AuthResult(userId, pojo.getDeviceType()));
        ensurePostAuthPipeline(ctx);
        ctx.fireChannelRead(pojo);
    }

    // ==================== 核心提取逻辑 ====================

    /**
     * 从 HTTP 请求中提取认证信息
     */
    private AuthResult extractFromHttpRequest(FullHttpRequest req) {
        String token = extractTokenFromHttp(req);
        String userId = validateToken(token);
        String deviceType = extractDeviceTypeFromHttp(req);
        return new AuthResult(userId, deviceType);
    }

    /**
     * 从 ByteBuf 中提取认证信息（零拷贝优化）
     */
    private AuthResult extractFromByteBuf(ByteBuf buf) {
        int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            return AuthResult.INVALID;
        }

        // 使用 nioBuffer 避免堆内存拷贝（如果底层支持）
        byte[] bytes;
        if (buf.hasArray()) {
            bytes = buf.array();
        } else {
            bytes = new byte[readableBytes];
            buf.getBytes(buf.readerIndex(), bytes);
        }

        // 根据配置的协议类型优先解析
        String token = null;
        String deviceType = null;

        if ("proto".equalsIgnoreCase(protocolType)) {
            // 优先 Proto，失败再尝试 JSON
            token = tryExtractTokenFromProto(bytes);
            deviceType = tryExtractDeviceTypeFromProto(bytes);
            if (token == null) {
                token = tryExtractTokenFromJson(bytes);
                deviceType = tryExtractDeviceTypeFromJson(bytes);
            }
        } else {
            // 优先 JSON，失败再尝试 Proto
            token = tryExtractTokenFromJson(bytes);
            deviceType = tryExtractDeviceTypeFromJson(bytes);
            if (token == null) {
                token = tryExtractTokenFromProto(bytes);
                deviceType = tryExtractDeviceTypeFromProto(bytes);
            }
        }

        String userId = validateToken(token);
        return new AuthResult(userId, deviceType);
    }

    /**
     * 从文本中提取认证信息
     */
    private AuthResult extractFromString(String text) {
        if (StringUtils.isBlank(text)) {
            return AuthResult.INVALID;
        }

        String trimmed = text.trim();
        String token = null;
        String deviceType = null;

        // 尝试 JSON 解析
        if (trimmed.startsWith("{")) {
            try {
                JsonNode node = MAPPER.readTree(trimmed);
                token = getTextValue(node, "token");
                deviceType = getTextValue(node, "deviceType");
            } catch (Exception ignored) {
            }
        }

        // 尝试 URL 参数格式 (token=xxx&deviceType=xxx)
        if (token == null) {
            token = extractUrlParam(trimmed, "token");
            deviceType = extractUrlParam(trimmed, "deviceType");
        }

        String userId = validateToken(token);
        return new AuthResult(userId, deviceType);
    }

    // ==================== HTTP 提取方法 ====================

    private String extractTokenFromHttp(FullHttpRequest req) {
        // 1. URL 参数
        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        List<String> tokens = decoder.parameters().get("token");
        if (tokens != null && !tokens.isEmpty()) {
            return tokens.get(0);
        }

        // 2. Authorization Header
        String auth = req.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (StringUtils.hasText(auth)) {
            String trimmed = auth.trim();
            if (trimmed.toLowerCase().startsWith(BEARER_PREFIX)) {
                return trimmed.substring(7).trim();
            }
            return trimmed;
        }

        // 3. Cookie
        String cookieHeader = req.headers().get(HttpHeaderNames.COOKIE);
        if (StringUtils.hasText(cookieHeader)) {
            Cookie cookie = ClientCookieDecoder.LAX.decode(cookieHeader);
            if (cookie != null && "token".equalsIgnoreCase(cookie.name())) {
                return cookie.value();
            }
        }

        return null;
    }

    private String extractDeviceTypeFromHttp(FullHttpRequest req) {
        // URL 参数优先
        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        List<String> types = decoder.parameters().get("deviceType");
        if (types != null && !types.isEmpty()) {
            return types.get(0);
        }

        // Header 备选
        return req.headers().get("X-Device-Type");
    }

    // ==================== Proto/JSON 解析方法 ====================

    private String tryExtractTokenFromProto(byte[] bytes) {
        try {
            IMessageProto.IMessageWrap proto = IMessageProto.IMessageWrap.parseFrom(bytes);
            String token = proto.getToken();
            return StringUtils.hasText(token) ? token : null;
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private String tryExtractDeviceTypeFromProto(byte[] bytes) {
        try {
            IMessageProto.IMessageWrap proto = IMessageProto.IMessageWrap.parseFrom(bytes);
            String dt = proto.getDeviceType();
            return StringUtils.hasText(dt) ? dt : null;
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private String tryExtractTokenFromJson(byte[] bytes) {
        try {
            String text = new String(bytes, StandardCharsets.UTF_8).trim();
            if (text.isEmpty() || !text.startsWith("{")) return null;

            JsonNode node = MAPPER.readTree(text);
            String token = getTextValue(node, "token");
            if (token != null) return token;

            // 尝试从嵌套 data 字段获取
            JsonNode data = node.get("data");
            return data != null ? getTextValue(data, "token") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String tryExtractDeviceTypeFromJson(byte[] bytes) {
        try {
            String text = new String(bytes, StandardCharsets.UTF_8).trim();
            if (text.isEmpty() || !text.startsWith("{")) return null;

            JsonNode node = MAPPER.readTree(text);
            return getTextValue(node, "deviceType");
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 工具方法 ====================

    private String validateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String userId = JwtUtil.getUsername(token);
        return StringUtils.hasText(userId) ? userId : null;
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && fieldNode.isTextual()) ? fieldNode.asText() : null;
    }

    private String extractUrlParam(String text, String paramName) {
        String search = paramName + "=";
        int idx = text.indexOf(search);
        if (idx < 0) return null;

        String sub = text.substring(idx + search.length());
        int ampIdx = sub.indexOf('&');
        return (ampIdx >= 0 ? sub.substring(0, ampIdx) : sub).trim();
    }

    private void bindUserAttributes(ChannelHandlerContext ctx, AuthResult result) {
        ctx.channel().attr(USER_ID_ATTR).set(result.userId);
        ctx.channel().attr(DEVICE_TYPE_ATTR).set(result.deviceType);
    }

    /**
     * 注入后续业务处理器并从 Pipeline 移除自身
     */
    private void ensurePostAuthPipeline(ChannelHandlerContext ctx) {
        ChannelPipeline pipeline = ctx.pipeline();

        if (pipeline.get(LoginHandler.class) == null) {
            pipeline.addLast("idle", new IdleStateHandler(0, 0, heartBeatTime, TimeUnit.MILLISECONDS));
            pipeline.addLast("heartBeat", heartBeatHandler);
            pipeline.addLast("login", loginHandler);
        }

        // 移除鉴权 Handler (已完成使命)
        try {
            pipeline.remove(this);
            log.debug("AuthHandler 已从 Pipeline 移除");
        } catch (NoSuchElementException ignored) {
        }
    }

    private void sendUnauthorizedAndClose(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    private void closeOnFailure(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("AuthHandler 异常: channelId={}, error={}", ctx.channel().id().asShortText(), cause.getMessage());
        closeOnFailure(ctx);
    }

    /**
     * 鉴权结果封装
     */
    private record AuthResult(String userId, String deviceType) {
        static final AuthResult INVALID = new AuthResult(null, null);

        boolean isValid() {
            return StringUtils.hasText(userId);
        }
    }
}
