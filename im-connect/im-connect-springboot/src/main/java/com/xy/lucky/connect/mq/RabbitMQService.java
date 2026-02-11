package com.xy.lucky.connect.mq;

import com.rabbitmq.client.Channel;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.domain.MessageEvent;
import com.xy.lucky.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 服务类
 * <p>
 * 使用 Spring AMQP 处理 RabbitMQ 消息发送和接收。
 * 从 YML 配置中获取服务名称，并结合 brokerId 动态注册队列。
 * 监听动态队列，处理消息并发布事件。
 *
 * @author Lucky
 */
@Slf4j(topic = LogConstant.Rabbit)
@Component
@RequiredArgsConstructor
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 使用机器码作为队列名称，保持与 core 模块一致
     */
    @Value("${brokerId}")
    private String queueName;

    /**
     * 业务交换机名称
     */
    @Value("${rabbitmq.exchange:IM-SERVER}")
    private String exchangeName;

    /**
     * 错误队列名称（同时作为错误 routingKey）
     */
    @Value("${rabbitmq.errorQueue:im.error}")
    private String errorRoutingKey;

    /**
     * 发送消息到 Broker
     *
     * @param routingKey 路由键
     * @param message    消息内容
     */
    public void sendToBroker(String routingKey, String message) {
        if (!StringUtils.hasText(routingKey) || !StringUtils.hasText(message)) {
            log.warn("路由键或消息内容为空，跳过发送: routingKey={}", routingKey);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            log.debug("消息发送成功: routingKey={}, message={}", routingKey, message);
        } catch (Exception e) {
            log.error("消息发送失败: routingKey={}, message={}", routingKey, message, e);
        }
    }

    /**
     * 发送错误消息到错误队列
     *
     * @param message 错误消息内容
     */
    public void sendErrorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            log.warn("错误消息内容为空，跳过发送");
            return;
        }
        sendToBroker(errorRoutingKey, message);
    }

    /**
     * 监听队列（手动 ack）
     *
     * @param message     接收到的消息内容
     * @param amqpMessage AMQP 消息对象
     * @param channel     底层 Channel，用于 ack/nack
     */
    @RabbitListener(queues = "${brokerId}", containerFactory = "rabbitListenerContainerFactory")
    public void handleMessage(String message, Message amqpMessage, Channel channel) {
        log.debug("收到 RabbitMQ 消息: queue={}, message={}", queueName, message);
        try {
            eventPublisher.publishEvent(new MessageEvent(message));
            Long tag = amqpMessage.getMessageProperties().getDeliveryTag();
            if (tag != null) {
                channel.basicAck(tag, false);
            }
        } catch (Exception e) {
            log.error("处理消息失败: queue={}, message={}", queueName, message, e);
            String errorMsg = String.format("msgId=%s, error=%s, payload=%s",
                    amqpMessage.getMessageProperties().getMessageId(),
                    e.getMessage(),
                    message);
            sendErrorMessage(errorMsg);
            try {
                Long tag = amqpMessage.getMessageProperties().getDeliveryTag();
                if (tag != null) {
                    channel.basicNack(tag, false, false);
                }
            } catch (Exception ex) {
                log.error("消息 nack 失败", ex);
            }
        }
    }

    public String getQueueName() {
        return queueName;
    }
}
