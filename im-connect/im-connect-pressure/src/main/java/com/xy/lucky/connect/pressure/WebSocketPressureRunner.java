package com.xy.lucky.connect.pressure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.lucky.connect.domain.proto.IMessageProto;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocketPressureRunner {
    private static final Logger log = LoggerFactory.getLogger(WebSocketPressureRunner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PressureTestConfig config;
    private final PressureMetrics metrics = new PressureMetrics();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ScheduledExecutorService scheduler;
    private final List<ClientSession> sessions;
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    private final AtomicInteger connectCursor = new AtomicInteger(0);
    private final AtomicLong requestSeq = new AtomicLong(0);

    private EventLoopGroup group;
    private Bootstrap bootstrap;
    private URI baseUri;

    public WebSocketPressureRunner(PressureTestConfig config) {
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(Math.max(4, Math.min(16, config.workerThreads())));
        this.sessions = buildSessions(config);
    }

    public void run() throws Exception {
        this.baseUri = URI.create(config.url());
        this.group = new NioEventLoopGroup(config.workerThreads());
        this.bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.connectTimeoutMs());
        log.info("pressure start, instanceTag={}, shard={}/{}, protocol={}, authMode={}, connections={}, connectRate={}/s",
                config.instanceTag(), config.shardIndex(), config.shardTotal(), config.protocol(), config.authMode(),
                config.connections(), config.connectRate());

        scheduler.scheduleAtFixedRate(() -> log.info("metrics: {}", metrics.snapshotLine()),
                config.printIntervalSeconds(),
                config.printIntervalSeconds(),
                TimeUnit.SECONDS);

        if (config.heartbeatIntervalMs() > 0) {
            scheduler.scheduleAtFixedRate(this::broadcastHeartbeat,
                    config.heartbeatIntervalMs(),
                    config.heartbeatIntervalMs(),
                    TimeUnit.MILLISECONDS);
        }

        if (config.messageIntervalMs() > 0) {
            scheduler.scheduleAtFixedRate(this::broadcastBusinessMessage,
                    config.messageIntervalMs(),
                    config.messageIntervalMs(),
                    TimeUnit.MILLISECONDS);
        }

        startConnectDispatcher();
        scheduler.schedule(this::stop, config.durationSeconds(), TimeUnit.SECONDS);
        doneLatch.await();
        log.info("pressure test completed, final metrics: {}", metrics.snapshotLine());
    }

    private void startConnectDispatcher() {
        int batchSize = Math.max(1, config.connectRate());
        scheduler.scheduleAtFixedRate(() -> {
            if (!running.get()) {
                return;
            }
            for (int i = 0; i < batchSize; i++) {
                int next = connectCursor.getAndIncrement();
                if (next >= sessions.size()) {
                    return;
                }
                connectSession(sessions.get(next));
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void connectSession(ClientSession session) {
        if (!running.get()) {
            return;
        }
        metrics.connectAttempt.increment();
        session.connectStartNanos = System.nanoTime();
        URI targetUri = buildUriForSession(session);
        DefaultHttpHeaders headers = buildHandshakeHeaders(session);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                targetUri,
                WebSocketVersion.V13,
                null,
                true,
                headers,
                config.maxFramePayloadLength()
        );

        Bootstrap perConnectionBootstrap = bootstrap.clone();
        perConnectionBootstrap.handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new ReadTimeoutHandler(Math.max(10, config.connectTimeoutMs() / 1000)));
                pipeline.addLast(new ClientWebSocketHandler(handshaker, session));
            }
        });

        perConnectionBootstrap.connect(targetUri.getHost(), resolvePort(targetUri)).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                metrics.connectFail.increment();
                scheduleReconnect(session);
            }
        });
    }

    private void scheduleReconnect(ClientSession session) {
        if (!running.get() || !config.autoReconnect()) {
            return;
        }
        metrics.reconnectScheduled.increment();
        int jitter = Math.floorMod(session.index, 500);
        scheduler.schedule(() -> connectSession(session), config.reconnectDelayMs() + jitter, TimeUnit.MILLISECONDS);
    }

    private void broadcastHeartbeat() {
        if (!running.get()) {
            return;
        }
        for (ClientSession session : sessions) {
            if (session.connected.get() && session.channel != null && session.channel.isActive()) {
                sendMessage(session, 206, "heartbeat", true);
            }
        }
    }

    private void broadcastBusinessMessage() {
        if (!running.get()) {
            return;
        }
        for (ClientSession session : sessions) {
            if (session.connected.get() && session.channel != null && session.channel.isActive()) {
                sendMessage(session, config.messageCode(), "pressure-message", false);
            }
        }
    }

    private void sendRegister(ClientSession session) {
        sendMessage(session, config.registerCode(), "register", false);
        metrics.registerSent.increment();
    }

    private void sendMessage(ClientSession session, int code, String message, boolean heartbeat) {
        WebSocketFrame frame = buildFrame(session, code, message);
        session.channel.writeAndFlush(frame).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                metrics.sendFail.increment();
            } else if (heartbeat) {
                metrics.heartbeatSent.increment();
            } else if (code == config.registerCode()) {
            } else {
                metrics.businessSent.increment();
            }
        });
    }

    private WebSocketFrame buildFrame(ClientSession session, int code, String message) {
        long now = Instant.now().toEpochMilli();
        String requestId = session.requestIdPrefix + "-" + requestSeq.incrementAndGet();
        if (config.protocol() == PressureTestConfig.Protocol.JSON) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("code", code);
            root.put("token", session.token);
            root.put("requestId", requestId);
            root.put("timestamp", now);
            root.put("message", message);
            root.put("deviceType", config.deviceType());
            root.put("metadata", Map.of("from", "im-connect-pressure", "clientIndex", String.valueOf(session.index)));
            try {
                return new TextWebSocketFrame(MAPPER.writeValueAsString(root));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        IMessageProto.IMessageWrap proto = IMessageProto.IMessageWrap.newBuilder()
                .setCode(code)
                .setToken(session.token)
                .setRequestId(requestId)
                .setTimestamp(now)
                .setMessage(message)
                .setDeviceType(config.deviceType())
                .putMetadata("from", "im-connect-pressure")
                .putMetadata("clientIndex", String.valueOf(session.index))
                .build();
        return new BinaryWebSocketFrame(io.netty.buffer.Unpooled.wrappedBuffer(proto.toByteArray()));
    }

    private int parseCode(WebSocketFrame frame) {
        try {
            if (frame instanceof TextWebSocketFrame textFrame) {
                JsonNode node = MAPPER.readTree(textFrame.text());
                return node.path("code").asInt(-1);
            }
            if (frame instanceof BinaryWebSocketFrame binaryFrame) {
                IMessageProto.IMessageWrap proto = IMessageProto.IMessageWrap.parseFrom(binaryFrame.content().nioBuffer());
                return proto.getCode();
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private URI buildUriForSession(ClientSession session) {
        if (config.authMode() == PressureTestConfig.AuthMode.URL) {
            String uri = appendQuery(baseUri.toString(), "token", session.token);
            uri = appendQuery(uri, "deviceType", config.deviceType());
            return URI.create(uri);
        }
        return baseUri;
    }

    private DefaultHttpHeaders buildHandshakeHeaders(ClientSession session) {
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        if (config.authMode() == PressureTestConfig.AuthMode.HEADER) {
            headers.set(HttpHeaderNames.AUTHORIZATION, "Bearer " + session.token);
            headers.set("X-Device-Type", config.deviceType());
        } else if (config.authMode() == PressureTestConfig.AuthMode.COOKIE) {
            headers.set(HttpHeaderNames.COOKIE, "token=" + session.token);
            headers.set("X-Device-Type", config.deviceType());
        }
        return headers;
    }

    private int resolvePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "wss".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String appendQuery(String url, String key, String value) {
        String sep = url.contains("?") ? "&" : "?";
        return url + sep + key + "=" + value;
    }

    private List<ClientSession> buildSessions(PressureTestConfig cfg) {
        List<ClientSession> list = new ArrayList<>(cfg.connections());
        for (int i = 0; i < cfg.connections(); i++) {
            int globalIndex = cfg.globalClientIndex(i);
            list.add(new ClientSession(globalIndex, cfg.tokenFor(globalIndex), cfg.instanceTag()));
        }
        return list;
    }

    private void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        for (ClientSession session : sessions) {
            Channel channel = session.channel;
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(new CloseWebSocketFrame());
                channel.close();
            }
        }
        scheduler.shutdownNow();
        if (group != null) {
            group.shutdownGracefully();
        }
        doneLatch.countDown();
    }

    private class ClientWebSocketHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private final ClientSession session;

        ClientWebSocketHandler(WebSocketClientHandshaker handshaker, ClientSession session) {
            this.handshaker = handshaker;
            this.session = session;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            session.channel = ctx.channel();
            handshaker.handshake(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            if (!handshaker.isHandshakeComplete()) {
                FullHttpResponse response = (FullHttpResponse) msg;
                handshaker.finishHandshake(ctx.channel(), response);
                long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - session.connectStartNanos);
                metrics.handshakeLatencyMsTotal.add(latencyMs);
                metrics.handshakeLatencyCount.increment();
                metrics.connectSuccess.increment();
                if (session.connected.compareAndSet(false, true)) {
                    metrics.activeConnections.incrementAndGet();
                }
                if (config.registerOnConnect()) {
                    sendRegister(session);
                }
                return;
            }

            if (msg instanceof WebSocketFrame frame) {
                metrics.received.increment();
                int code = parseCode(frame);
                if (code == 209) {
                    metrics.registerAck.increment();
                } else if (code == 207) {
                    metrics.heartbeatAck.increment();
                }
                if (frame instanceof CloseWebSocketFrame) {
                    ctx.close();
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (session.connected.compareAndSet(true, false)) {
                metrics.activeConnections.decrementAndGet();
            }
            scheduleReconnect(session);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            metrics.connectFail.increment();
            ctx.close();
        }
    }

    private static class ClientSession {
        final int index;
        final String token;
        final String requestIdPrefix;
        final AtomicBoolean connected = new AtomicBoolean(false);
        volatile Channel channel;
        volatile long connectStartNanos;

        ClientSession(int index, String token, String instanceTag) {
            this.index = index;
            this.token = token;
            this.requestIdPrefix = instanceTag + "-" + index;
        }
    }
}
