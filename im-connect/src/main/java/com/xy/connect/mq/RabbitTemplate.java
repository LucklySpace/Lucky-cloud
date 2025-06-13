package com.xy.connect.mq;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import com.xy.connect.config.LogConstant;
import com.xy.connect.domain.MessageEvent;
import com.xy.spring.XSpringApplication;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.PostConstruct;
import com.xy.spring.annotations.core.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

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
    @Value("rabbitmq.address")
    private String host;
    @Value("rabbitmq.port")
    private int port;
    @Value("rabbitmq.username")
    private String userName;
    @Value("rabbitmq.password")
    private String password;
    @Value("rabbitmq.virtual")
    private String virtualHost;
    @Value("brokerId")
    private String queueName;
    // 可配置交换机、路由键前缀和错误队列名称
    @Value("rabbitmq.exchange")
    private String exchangeName = "im.exchange";
    @Value("rabbitmq.routingKeyPrefix")
    private String routingKeyPrefix = "im.router.";
    @Value("rabbitmq.errorQueue")
    private String errorQueue = "error.queue";
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private volatile boolean ready = false;


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

        // 设置连接异常处理器
        factory.setExceptionHandler(new DefaultExceptionHandler() {
            @Override
            public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
                log.warn("检测到 RabbitMQ 连接丢失，准备重连...", exception);
                ready = false;
                closeResources(); // 关闭资源
                startConsumer();  // 尝试重连
            }
        });
        log.info("RabbitMQ ConnectionFactory 构建完成 -> {}:{}", host, port);
    }

    /**
     * 启动消费者监听队列
     */
    private synchronized void startConsumer() {
        if (ready) {
            log.debug("RabbitMQ 已在运行，跳过启动");
            return;
        }
        try {
            // 创建连接与通道
            connection = factory.newConnection();
            channel = connection.createChannel();

            // 声明交换机，如果不存在则自动创建
            channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, true);

            /**
             * 创建队列并绑定到交换机 （如果不存在则自动创建）
             * 1. queue： 队列的名称
             * 2. durable： 是否持久化
             * 3. exclusive： 是否独占,只能被一个连接使用,连接关闭后队列自动删除
             * 4. autoDelete： 是否自动删除,当没有消费者时,队列自动删除,确保队列唯一性
             * 5. arguments： 其他参数
             */
            channel.queueDeclare(queueName, true, true, true, null);

            // 将队列绑定到交换机
            channel.queueBind(queueName, exchangeName, routingKeyPrefix + queueName);


            // 创建消费者来处理消息
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    handleDeliveryClient(consumerTag, envelope, properties, body);
                }
            };

            // 开始消费消息，自动确认  autoAck 设置为 false
            channel.basicConsume(queueName, false, consumer);

            ready = true;
            log.info("RabbitMQ 队列 {} 监听启动成功", queueName);
        } catch (Exception e) {
            log.error("RabbitMQ 启动监听失败，稍后重试", e);
            safeSleep(5000);
            startConsumer(); // 简单重试
        }
    }

    /**
     * 消息处理回调逻辑
     *
     * @param consumerTag 消费者标签
     * @param envelope    消息信封，含路由信息
     * @param properties  消息属性
     * @param body        消息体字节数组
     */
    private void handleDeliveryClient(String consumerTag,
                                      Envelope envelope,
                                      AMQP.BasicProperties properties,
                                      byte[] body) throws IOException {
        // 通用执行模板，捕获异常并自动确认/拒绝消息
        executeWithChannel(ch -> {

            // 消息转换
            String context = new String(body, StandardCharsets.UTF_8);

            log.info("rabbitmq收到消息 {} exchange={}, routingKey={}", context, envelope.getExchange(), envelope.getRoutingKey());

            try {

                // 事件发布
                XSpringApplication.getContext().getEventPublisher().publishEvent(new MessageEvent(context));

                // 成功处理后手动 ack
                ch.basicAck(envelope.getDeliveryTag(), false);

            } catch (Exception procEx) {

                log.error("消息处理失败", procEx);

                // 发送错误信息到错误队列
                sendErrorMessage(envelope, body, procEx.getMessage());

                // 拒绝此消息，不重新入队
                ch.basicNack(envelope.getDeliveryTag(), false, false);
            }
        });
    }

    /**
     * 通用执行模板：执行操作前校验通道可用，并处理异常
     */
    private void executeWithChannel(ChannelConsumer consumer) {
        try {
            // 若 channel 不可用则重新启动监听
            if (!ready || channel == null || !channel.isOpen()) {
                startConsumer();
            }
            consumer.accept(channel); // 执行实际逻辑
        } catch (IOException | TimeoutException e) {
            log.error("Channel 操作异常，准备重连", e);
            ready = false;
            closeResources();
            startConsumer(); // 自动重连
        }
    }

    /**
     * 发送错误消息到预定义错误队列
     */
    private void sendErrorMessage(Envelope env, byte[] body, String errorMsg) {
        String fullMsg = String.format("msgId=%d, error=%s, payload=%s",
                env.getDeliveryTag(), errorMsg, new String(body, StandardCharsets.UTF_8));

        executeWithChannel(ch -> {
            ch.basicPublish(exchangeName, errorQueue, null, fullMsg.getBytes(StandardCharsets.UTF_8));
            log.info("错误消息已发送到队列 {}", errorQueue);
        });
    }

    /**
     * 安全关闭连接与通道资源
     */
    private void closeResources() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
                log.info("RabbitMQ Channel 已关闭");
            }
        } catch (Exception e) {
            log.warn("关闭 Channel 失败", e);
        } finally {
            channel = null;
        }

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
                log.info("RabbitMQ Connection 已关闭");
            }
        } catch (Exception e) {
            log.warn("关闭 Connection 失败", e);
        } finally {
            connection = null;
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
