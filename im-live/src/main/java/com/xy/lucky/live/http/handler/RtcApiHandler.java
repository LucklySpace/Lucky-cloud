package com.xy.lucky.live.http.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.live.http.RtcApiResponse;
import com.xy.lucky.live.http.RtcApiService;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RTC API 请求处理器
 * <p>
 * 处理 SRS 兼容的 WebRTC 推拉流 HTTP API 请求。
 *
 * @author lucky
 * @version 1.0.0
 */
public class RtcApiHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(RtcApiHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final RtcApiService rtcApiService;

    public RtcApiHandler(RtcApiService rtcApiService) {
        this.rtcApiService = rtcApiService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // 处理 OPTIONS 请求（CORS 预检）
            if (request.method() == HttpMethod.OPTIONS) {
                sendCorsResponse(ctx, request);
                return;
            }

            // 只处理 POST 请求
            if (request.method() != HttpMethod.POST) {
                sendError(ctx, request, HttpResponseStatus.METHOD_NOT_ALLOWED, "Only POST method is allowed");
                return;
            }

            // 解析 URI
            String uri = request.uri();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(uri);
            String path = queryDecoder.path();

            // 解析请求体
            String content = request.content().toString(StandardCharsets.UTF_8);
            Map<String, Object> requestBody;
            try {
                requestBody = objectMapper.readValue(content, Map.class);
            } catch (Exception e) {
                sendError(ctx, request, HttpResponseStatus.BAD_REQUEST, "Invalid JSON format");
                return;
            }

            // 获取 API 和 SDP
            String api = (String) requestBody.get("api");
            String sdp = (String) requestBody.get("sdp");

            if (api == null || sdp == null) {
                sendError(ctx, request, HttpResponseStatus.BAD_REQUEST, "Missing 'api' or 'sdp' field");
                return;
            }

            // 根据路径分发到不同的处理器
            RtcApiResponse response;
            if (path.endsWith("/publish/")) {
                response = rtcApiService.handlePublish(api, sdp);
            } else if (path.endsWith("/play/")) {
                response = rtcApiService.handlePlay(api, sdp);
            } else {
                sendError(ctx, request, HttpResponseStatus.NOT_FOUND, "API path not found: " + path);
                return;
            }

            // 发送响应
            sendResponse(ctx, request, response);

        } catch (Exception e) {
            log.error("处理 RTC API 请求异常", e);
            sendError(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * 发送响应
     */
    private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest request, RtcApiResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    response.getCode() == 0 ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST,
                    Unpooled.wrappedBuffer(bytes)
            );

            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
            httpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            httpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST, OPTIONS");
            httpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");

            ctx.writeAndFlush(httpResponse);
        } catch (Exception e) {
            log.error("发送响应异常", e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendError(ChannelHandlerContext ctx, FullHttpRequest request,
                           HttpResponseStatus status, String message) {
        try {
            RtcApiResponse response = RtcApiResponse.error(status.code(), message);
            String json = objectMapper.writeValueAsString(response);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            FullHttpResponse httpResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes)
            );

            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
            httpResponse.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

            ctx.writeAndFlush(httpResponse);
        } catch (Exception e) {
            log.error("发送错误响应异常", e);
        }
    }

    /**
     * 发送 CORS 预检响应
     */
    private void sendCorsResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK
        );

        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "3600");

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RTC API 处理器异常", cause);
        ctx.close();
    }
}

