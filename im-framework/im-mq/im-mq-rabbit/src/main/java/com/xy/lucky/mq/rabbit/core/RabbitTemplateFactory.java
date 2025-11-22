package com.xy.lucky.mq.rabbit.core;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnsCallback;
import org.springframework.stereotype.Component;

@Component
public class RabbitTemplateFactory {

    private final ConnectionFactory connectionFactory;

    public RabbitTemplateFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public RabbitTemplate createRabbitTemplate(ConfirmCallback confirmCallback, ReturnsCallback returnsCallback) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        //Mandatory为true时,消息通过交换器无法匹配到队列会返回给生产者，为false时匹配不到会直接被丢弃
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setConfirmCallback(confirmCallback);
        rabbitTemplate.setReturnsCallback(returnsCallback);

        return rabbitTemplate;
    }
}

