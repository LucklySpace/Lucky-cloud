package com.xy.lucky.connect.config;

import com.xy.lucky.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * <p>
 * - 连接工厂由 Spring Boot 自动配置（spring.rabbitmq.*）
 * - 本类只负责：消息转换器 / RabbitTemplate / Listener 工厂 / 交换机&队列声明
 *
 * @author Lucky
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    @Value("${brokerId:}")
    private String brokerId;

    /**
     * 业务交换机名称
     */
    @Value("${rabbitmq.exchange:IM-SERVER}")
    private String exchangeName;

    /**
     * 错误队列名称（同时作为错误 routingKey）
     */
    @Value("${rabbitmq.errorQueue:im.error}")
    private String errorQueueName;

    /**
     * 配置消息转换器（JSON）
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate，并开启 confirm / return 回调
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("RabbitMQ 消息发送失败: correlationData={}, cause={}", correlationData, cause);
            }
        });

        template.setReturnsCallback(returned -> log.warn(
                "RabbitMQ 消息返回: message={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                returned.getExchange(), returned.getRoutingKey()));

        log.info("RabbitTemplate 配置完成");
        return template;
    }

    /**
     * 配置 RabbitListener 容器工厂（手动 ACK）
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        log.info("RabbitListener 容器工厂配置完成");
        return factory;
    }

    /**
     * 声明业务交换机
     */
    @Bean
    public DirectExchange imServerExchange() {
        DirectExchange exchange = ExchangeBuilder
                .directExchange(exchangeName)
                .durable(true)
                .build();
        log.info("声明交换机: {}", exchangeName);
        return exchange;
    }

    /**
     * 声明业务队列（使用 brokerId 作为队列名）
     */
    @Bean
    public Queue imConnectQueue() {
        if (!StringUtils.hasText(brokerId)) {
            log.warn("brokerId 为空，无法创建队列");
            return null;
        }

        Queue queue = QueueBuilder
                .durable(brokerId)
                .exclusive()
                .autoDelete()
                .build();
        log.info("声明队列: {}", brokerId);
        return queue;
    }

    /**
     * 绑定业务队列到交换机（routingKey = brokerId）
     */
    @Bean
    public Binding imConnectBinding(Queue imConnectQueue,
                                    DirectExchange imServerExchange) {
        if (imConnectQueue == null) {
            return null;
        }
        Binding binding = BindingBuilder
                .bind(imConnectQueue)
                .to(imServerExchange)
                .with(brokerId);
        log.info("绑定队列到交换机: queue={}, exchange={}, routingKey={}",
                brokerId, imServerExchange.getName(), brokerId);
        return binding;
    }

    /**
     * 绑定错误队列到交换机（routingKey = 错误队列名）
     */
    @Bean
    public Binding errorQueueBinding(Queue errorQueue,
                                     DirectExchange imServerExchange) {
        Binding binding = BindingBuilder
                .bind(errorQueue)
                .to(imServerExchange)
                .with(errorQueueName);
        log.info("绑定错误队列到交换机: queue={}, exchange={}, routingKey={}",
                errorQueueName, imServerExchange.getName(), errorQueueName);
        return binding;
    }
}
