package com.xy.lucky.connect.constant;

/**
 * IM 连接服务常量定义
 *
 * @author Lucky
 */
public final class ConnectConstants {

    private ConnectConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * Redis 相关常量
     */
    public static final class Redis {
        public static final int DEFAULT_TIMEOUT = 10000;
        public static final int DEFAULT_MAX_TOTAL = 100;
        public static final int DEFAULT_MAX_IDLE = 10;
        public static final int DEFAULT_DB_INDEX = 0;

        private Redis() {
        }
    }

    /**
     * RabbitMQ 相关常量
     */
    public static final class RabbitMQ {
        public static final int DEFAULT_PREFETCH = 50;
        public static final long DEFAULT_RECOVERY_INTERVAL = 5000L;
        public static final String DEFAULT_VIRTUAL_HOST = "/";
        public static final String DEFAULT_EXCHANGE = "im.exchange";
        public static final String DEFAULT_ROUTING_KEY_PREFIX = "im.router.";
        public static final String DEFAULT_ERROR_QUEUE = "error.queue";

        private RabbitMQ() {
        }
    }

    /**
     * 限流相关常量
     */
    public static final class RateLimit {
        // 连接限流
        public static final int DEFAULT_MAX_CONNECTIONS_PER_USER = 5;
        public static final int DEFAULT_MAX_GLOBAL_CONNECTIONS = 10000;

        // 消息限流
        public static final long DEFAULT_WINDOW_SIZE_MS = 1000L;
        public static final int DEFAULT_MAX_MESSAGES_PER_WINDOW = 20;

        // 清理任务
        public static final long CLEANUP_INITIAL_DELAY_MS = 60000L;
        public static final long CLEANUP_INTERVAL_MS = 300000L;

        private RateLimit() {
        }
    }

    /**
     * Netty 相关常量
     */
    public static final class Netty {
        public static final int DEFAULT_BOSS_THREADS = 4;
        public static final int DEFAULT_WORKER_THREADS = 16;
        public static final int DEFAULT_HEARTBEAT_TIME = 30000;
        public static final String DEFAULT_WS_PATH = "/im";

        private Netty() {
        }
    }

    /**
     * 监控相关常量
     */
    public static final class Monitoring {
        public static final String COUNTER_GLOBAL_LIMIT = "limiter.global_limit_reached";
        public static final String COUNTER_USER_LIMIT = "limiter.user_limit_reached";
        public static final String COUNTER_RATE_LIMIT = "rate_limiter.limit_exceeded";

        public static final String METRIC_USER_COUNT = "connections.user_count";
        public static final String METRIC_GLOBAL_COUNT = "connections.global_count";

        private Monitoring() {
        }
    }

    /**
     * Channel 相关常量
     */
    public static final class Channel {
        public static final String CLOSE_SUCCESS_LOG = "被替换旧连接已关闭: userId={}, deviceType={}, prevChannelId={}, success={}";
        public static final String BIND_LOG = "添加用户通道: userId={}, deviceType={}, channelId={}";
        public static final String REMOVE_LOG = "快速移除通道映射: userId={}, deviceType={}, channelId={}";

        private Channel() {
        }
    }

    /**
     * 业务相关常量
     */
    public static final class Business {
        public static final String DEFAULT_DEVICE_TYPE = "WEB";
        public static final int AUTH_TOKEN_EXPIRED_DAYS = 3;

        private Business() {
        }
    }
}

