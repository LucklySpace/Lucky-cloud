package com.xy.connect.mq;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.MessageHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.xy.imcore.constants.Constant.EXCHANGENAME;
import static com.xy.imcore.constants.Constant.ROUTERKEYPREFIX;

/**
 * RabbitMQ连接客户端工具类
 * https://blog.csdn.net/u010989191/article/details/112220574#:~:text=1.%E5%BC%95%E8%A8%80%20R
 * @author dense
 */
@Slf4j(topic = LogConstant.RABBITMQ)
public class RabbitMQHandler implements Runnable {

    private final MessageHandler messageHandler;
    private final String host;
    private final int port;
    private final String userName;
    private final String password;
    private final String virtualHost;
    private final String queueName;
    private final ExecutorService executorService;
    private Connection connection = null;
    private Channel channel = null;
    private volatile boolean isConnected = false;

    public RabbitMQHandler(String host, int port, String queueName, MessageHandler messageHandler) {
        this(host, port, "", "", "", queueName, messageHandler);
    }

    public RabbitMQHandler(String host, int port, String virtualHost, String queueName, MessageHandler messageHandler) {
        this(host, port, "", "", virtualHost, queueName, messageHandler);
    }

    public RabbitMQHandler(String host, int port, String userName, String password, String virtualHost, String queueName, MessageHandler messageHandler) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.virtualHost = virtualHost;
        this.queueName = queueName;
        this.messageHandler = messageHandler;
        this.executorService = Executors.newSingleThreadExecutor();
        this.executorService.submit(this);
    }

    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 启动监听
     */
    public void start() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);

            // 如果用户名和密码不为空，则设置认证
            if (StrUtil.isNotBlank(userName)) {
                factory.setUsername(userName);
            }
            if (StrUtil.isNotBlank(password)) {
                factory.setPassword(password);
            }
            if (StrUtil.isNotBlank(virtualHost)) {
                factory.setVirtualHost(virtualHost);
            }

            // 设置异常处理器，处理自动恢复的连接问题
            factory.setExceptionHandler(new DefaultExceptionHandler() {
                @Override
                public void handleConnectionRecoveryException(Connection conn, Throwable exception) {
                    super.handleConnectionRecoveryException(conn, exception);
                    log.warn("自动检测到连接断开，尝试重连...");
                    close();
                    isConnected = false;
                }
            });

            // 创建连接和通道
            connection = factory.newConnection();
            channel = connection.createChannel();

            // 声明交换机，如果不存在则自动创建
            channel.exchangeDeclare(EXCHANGENAME, BuiltinExchangeType.DIRECT, true);

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
            channel.queueBind(queueName, EXCHANGENAME, ROUTERKEYPREFIX + queueName);

            // 创建消费者来处理消息
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    handleDeliveryClient(consumerTag, envelope, properties, body);
                }
            };

            // 开始消费消息，自动确认
            channel.basicConsume(queueName, false, consumer);  // autoAck 设置为 false
            //channel.basicConsume(queueName, true, consumer);

            isConnected = true;
            log.info("RabbitMQ 队列 {} 启动成功", queueName);
        } catch (Exception e) {
            log.error("启动 RabbitMQ 监听异常", e);
            isConnected = false;
            close();
        }
    }

    /**
     * 处理消息数据
     *
     * @param consumerTag 消费者标签
     * @param envelope    信封，包含交换机、路由键等信息
     * @param properties  消息属性
     * @param body        消息体
     * @throws IOException 处理消息过程中可能抛出的IO异常
     */
    public void handleDeliveryClient(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        try {
            log.info("收到消息 -> exchange: {}, routingKey: {}, queueName: {}", envelope.getExchange(), envelope.getRoutingKey(), this.queueName);

            if (this.messageHandler != null) {
                this.messageHandler.handlerMessage(body);
            }

            // 手动确认消息
            channel.basicAck(envelope.getDeliveryTag(), false);  // 确认当前消息已被处理

        } catch (Exception e) {
            log.error("RabbitMQ 消息解析异常", e);
            // 发送错误消息到生产者的错误队列
            sendErrorMessageToProducer(envelope, body, e.getMessage());

            // 你可以选择直接拒绝消息而不返回到队列
            channel.basicNack(envelope.getDeliveryTag(), false, false);  // 直接丢弃消息，不重新入队
        }
    }

    /**
     * 将错误信息发送回生产者
     *
     * @param envelope     信封
     * @param body         原始消息体
     * @param errorMessage 错误消息
     */
    private void sendErrorMessageToProducer(Envelope envelope, byte[] body, String errorMessage) {
        try {
            String errorQueue = "error.queue";  // 定义一个错误队列
            String errorMessageBody = String.format("处理消息失败，原始消息ID: %s, 错误信息: %s, 消息体: %s", envelope.getDeliveryTag(), errorMessage, new String(body, StandardCharsets.UTF_8));

            // 将错误消息发送到错误队列
            channel.basicPublish(EXCHANGENAME, errorQueue, null, errorMessageBody.getBytes(StandardCharsets.UTF_8));
            log.info("错误信息已发送到生产者: {}", errorMessageBody);

        } catch (IOException ioException) {
            log.error("发送错误消息回生产者失败", ioException);
        }
    }

    /**
     * 释放资源
     */
    private void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.basicCancel(queueName);
                channel.close();
                log.info("RabbitMQ Channel 关闭成功");
            }
        } catch (IOException | TimeoutException e) {
            log.warn("关闭 Channel 异常", e);
        } finally {
            channel = null;
        }

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
                log.info("RabbitMQ Connection 关闭成功");
            }
        } catch (IOException e) {
            log.warn("关闭 Connection 异常", e);
        } finally {
            connection = null;
        }

        isConnected = false;
        log.info("RabbitMQ 队列 {} 已关闭", queueName);
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!isConnected) {
                    log.info("RabbitMQ 队列 {} 正在启动...", queueName);
                    start();
                } else {
                    log.debug("RabbitMQ 队列 {} 正在运行", queueName);
                }
                Thread.sleep(1000 * 60);
            } catch (InterruptedException e) {
                log.error("RabbitMQ 运行异常", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}