package com.xy.connect.mq;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import com.xy.connect.config.LogConstant;
import com.xy.connect.domain.MessageEvent;
import com.xy.spring.annotations.core.*;
import com.xy.spring.event.ApplicationEventBus;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RabbitMQ连接客户端工具类
 * https://blog.csdn.net/u010989191/article/details/112220574#:~:text=1.%E5%BC%95%E8%A8%80%20R
 *
 * @author dense
 */
@Slf4j(topic = LogConstant.Rabbit)
@Component
public class RabbitTemplate {

    // -------------------- 配置注入 --------------------
    @Value("${rabbitmq.address}")
    private String host;
    @Value("${rabbitmq.port}")
    private int port;
    @Value("${rabbitmq.username:}")
    private String userName;
    @Value("${rabbitmq.password:}")
    private String password;
    @Value("${rabbitmq.virtual:/}")
    private String virtualHost;
    @Value("${brokerId}")
    private String queueName;

    @Value("${rabbitmq.exchange:im.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.routingKeyPrefix:im.router.}")
    private String routingKeyPrefix;

    @Value("${rabbitmq.errorQueue:error.queue}")
    private String errorQueue;

    @Value("${rabbitmq.prefetch:50}")
    private Integer prefetch = 50;


    private ConnectionFactory factory;
    private Connection connection;
    // 用于消费（ack/nack）
    private volatile Channel consumerChannel;
    // 用于发送错误消息等
    private volatile Channel publishChannel;

    // 状态
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    private ApplicationEventBus applicationEventBus;


    /**
     * 初始化方法：启动容器后自动调用
     */
    @PostConstruct
    public void init() {
        buildConnectionFactory();
        startConsumer(); // 启动消费者监听
    }

    /**
     * 构建 ConnectionFactory 并设置连接参数和异常处理器
     */
    private void buildConnectionFactory() {
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);

        if (StrUtil.isNotBlank(userName)) factory.setUsername(userName);
        if (StrUtil.isNotBlank(password)) factory.setPassword(password);
        if (StrUtil.isNotBlank(virtualHost)) factory.setVirtualHost(virtualHost);

        // 启用自动重连（client 端自动恢复连接与通道拓扑）
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        // 客户端自动恢复间隔
        factory.setNetworkRecoveryInterval(5000);

        // 设置连接异常处理器
        factory.setExceptionHandler(new DefaultExceptionHandler() {
            @Override
            public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
                log.warn("RabbitMQ 连接丢失，准备重连...", exception);
                running.set(false);
                // 关闭资源
                closeResourcesSafely();
                // 尝试重连
                startConsumer();
            }

            @Override
            public void handleChannelRecoveryException(Channel ch, Throwable exception) {
                log.warn("RabbitMQ channel 异常...", exception);
            }
        });
        log.info("RabbitMQ ConnectionFactory 构建完成 -> {}:{}", host, port);
    }

    /**
     * 启动消费者监听队列
     */
    private synchronized void startConsumer() {
        if (running.get()) {
            log.debug("RabbitMQ 已在运行，跳过启动");
            return;
        }
        try {
            // 创建连接与通道
            connection = factory.newConnection();

            // consumerChannel 专用于消费、ack/nack
            consumerChannel = connection.createChannel();

            // publishChannel 专用于发布（error queue 等）
            publishChannel = connection.createChannel();

            // declare exchange & queues（持久、非独占、非自动删除）声明交换机，如果不存在则自动创建
            consumerChannel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, true);

            /**
             * 创建队列并绑定到交换机 （如果不存在则自动创建）
             * 1. queue： 队列的名称
             * 2. durable： 是否持久化
             * 3. exclusive： 是否独占,只能被一个连接使用,连接关闭后队列自动删除
             * 4. autoDelete： 是否自动删除,当没有消费者时,队列自动删除,确保队列唯一性
             * 5. arguments： 其他参数
             */
            consumerChannel.queueDeclare(queueName, true, true, true, null);
            consumerChannel.queueBind(queueName, exchangeName, queueName);

            // declare error queue & bind (durable)
            publishChannel.queueDeclare(errorQueue, true, false, false, null);
            publishChannel.queueBind(errorQueue, exchangeName, errorQueue);

            // QoS 控制
            consumerChannel.basicQos(prefetch);

            /**
             * 消息处理回调逻辑 创建消费者来处理消息
             *
             * @param consumerTag 消费者标签
             * @param envelope    消息信封，含路由信息
             * @param properties  消息属性
             * @param body        消息体字节数组
             */
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                final long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                final byte[] body = delivery.getBody();

                // 业务处理提交到 workerPool，处理结束后再 ack/nack
                boolean success = false;
                try {

                    String context = new String(body, StandardCharsets.UTF_8);

                    // 发布事件（可被同步或异步处理）
                    applicationEventBus.publishEvent(new MessageEvent(context));

                    success = true;
                } catch (Throwable t) {
                    log.error("Failed to process message", t);
                    // 发送错误信息到 errorQueue
                    try {
                        sendErrorMessageSynchronized(delivery.getEnvelope(), body, t.getMessage());
                    } catch (Exception ex) {
                        log.error("Failed to send error message", ex);
                    }
                } finally {
                    // ack/nack 需要在对 consumerChannel 的同步保护下执行（channel 线程不安全）
                    synchronized (consumerChannel) {
                        try {
                            if (success) {
                                consumerChannel.basicAck(deliveryTag, false);
                            } else {
                                // nack 不重入队
                                consumerChannel.basicNack(deliveryTag, false, false);
                            }
                        } catch (IOException e) {
                            log.error("Failed to ack/nack message", e);
                        }
                    }
                }
            };

            CancelCallback cancelCallback = consumerTag -> log.warn("Consumer cancelled: {}", consumerTag);

            // 开始消费消息，自动确认 (autoAck = false)
            consumerChannel.basicConsume(queueName, false, deliverCallback, cancelCallback);

            running.set(true);

            log.info("RabbitMQ 队列 {} 监听启动成功", queueName);

        } catch (Exception e) {
            log.error("RabbitMQ 启动监听失败，稍后重试", e);
            safeSleep(5000);
            startConsumer(); // 简单重试
        }
    }


    /**
     * 关闭并清理资源（safe）
     */
    private synchronized void closeResourcesSafely() {
        running.set(false);
        try {
            if (consumerChannel != null) {
                try {
                    consumerChannel.close();
                    log.info("Consumer channel closed");
                } catch (Exception ignored) {
                }
                consumerChannel = null;
            }
        } finally {
            try {
                if (publishChannel != null) {
                    try {
                        publishChannel.close();
                        log.info("Publish channel closed");
                    } catch (Exception ignored) {
                    }
                    publishChannel = null;
                }
            } finally {
                try {
                    if (connection != null) {
                        try {
                            connection.close();
                            log.info("Connection closed");
                        } catch (Exception ignored) {
                        }
                        connection = null;
                    }
                } catch (Throwable t) {
                    log.warn("Error while closing connection", t);
                }
            }
        }
    }

    /**
     * 优雅关闭（容器销毁时调用）
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RabbitTemplate");

        //
        closeResourcesSafely();

        log.info("RabbitTemplate shutdown complete");
    }

    /**
     * 发送错误消息到 errorQueue，使用 publishChannel（同步保护）
     */
    private void sendErrorMessageSynchronized(Envelope env, byte[] body, String errorMsg) {
        if (publishChannel == null) {
            log.warn("publishChannel is null, cannot send error message");
            return;
        }
        String fullMsg = String.format("msgId=%d, error=%s, payload=%s",
                env.getDeliveryTag(), errorMsg, new String(body, StandardCharsets.UTF_8));
        byte[] bytes = fullMsg.getBytes(StandardCharsets.UTF_8);

        synchronized (publishChannel) {
            try {
                // 通过 exchange + routingKey（errorQueue）
                publishChannel.basicPublish(exchangeName, errorQueue, null, bytes);
                log.info("Error message published to queue {}", errorQueue);
            } catch (IOException e) {
                log.error("Failed to publish error message", e);
            }
        }
    }

    /**
     * 睡眠辅助方法，避免快速重试导致 CPU 飙高
     */
    private void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 函数式接口：封装 Channel 相关逻辑
     */
    @FunctionalInterface
    private interface ChannelConsumer {
        void accept(Channel channel) throws IOException, TimeoutException;
    }
}
