package com.xy.lucky.connect.config;

/**
 * 日志主题常量定义
 * 用于分类记录不同模块的日志
 * 对应日志文件配置：logback.xml
 *
 * @author Lucky
 */
public final class LogConstant {

    /**
     * 主应用日志
     */
    public static final String Main = "IM-Connect";
    /**
     * Netty 网络层日志
     */
    public static final String Netty = "netty";
    /**
     * RabbitMQ 消息队列日志
     */
    public static final String Rabbit = "rabbit";
    /**
     * Redis 缓存日志
     */
    public static final String Redis = "redis";
    /**
     * Spring 框架日志
     */
    public static final String Spring = "spring";
    /**
     * Nacos 服务发现日志
     */
    public static final String Nacos = "nacos";
    /**
     * 消息处理日志
     */
    public static final String Message = "message";
    /**
     * 心跳检测日志
     */
    public static final String HeartBeat = "heartBeat";
    /**
     * 认证授权日志
     */
    public static final String Auth = "auth";
    /**
     * 登录日志
     */
    public static final String Login = "login";
    /**
     * Channel 管理日志
     */
    public static final String Channel = "channel";
    /**
     * 登出日志
     */
    public static final String Logout = "logout";
    /**
     * 监控服务日志
     */
    public static final String Monitoring = "monitoring";
    /**
     * 异常处理日志
     */
    public static final String Exception = "exception";
    /**
     * 系统日志
     */
    public static final String System = "system";
    /**
     * 限流器日志
     */
    public static final String Limiter = "rateLimiter";

    private LogConstant() {
        throw new UnsupportedOperationException("常量类不允许实例");
    }
}
