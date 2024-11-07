package com.xy.server.service.impl;


import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.model.IMVideoMessageDto;
import com.xy.imcore.model.IMessageWrap;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.service.VideoChatService;
import com.xy.server.utils.JsonUtil;
import com.xy.server.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.xy.imcore.constants.Constant.*;

@Slf4j
@Service
public class VideoChatServiceImpl implements VideoChatService {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;

    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate((correlationData, ack, cause) -> {
            if (ack) {
                log.info("video消息成功发送到交换机，消息ID: {}", correlationData != null ? correlationData.getId() : null);
            } else {
                log.error("video消息发送到交换机失败，原因: {}", cause);
                // 可以在此处做消息的重发或其他处理
            }
        }, (returnedMessage) -> {
            log.info("RabbitMQ-video交换机到队列退回:::return callback 消息体：{},应答码:{},原因:{},交换机:{},路由键:{}",
                    returnedMessage.getMessage(), returnedMessage.getReplyCode(), returnedMessage.getReplyText(),
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        });
    }


    @Override
    @Transactional
    public void send(IMVideoMessageDto IMVideoMessageDto) {

        // 通过redis获取用户连接netty的机器码
        Object redisObj = redisUtil.get(IMUSERPREFIX + IMVideoMessageDto.getTo_id());

        if (ObjectUtil.isNotEmpty(redisObj)) {

            IMRegisterUserDto IMRegisterUserDto = JsonUtil.parseObject(redisObj, new TypeReference<IMRegisterUserDto>() {
            });

            String broker_id = IMRegisterUserDto.getBroker_id();
            // 对发送消息进行包装
            IMessageWrap IMessageWrap = new IMessageWrap(IMessageType.VIDEO_MESSAGE.getCode(), IMVideoMessageDto);

            // 发送到消息队列
            rabbitTemplate.convertAndSend(EXCHANGENAME, ROUTERKEYPREFIX + broker_id, JsonUtil.toJSONString(IMessageWrap));
        } else {
            log.info("用户:{} 未登录", IMVideoMessageDto.getTo_id());
        }
    }

//    public CorrelationData setMessageAck(String message_id){
//
//        rabbitTemplate.setMandatory(true);
//
//        rabbitTemplate.setConfirmCallback(this);
//
//        CorrelationData correlationData = new CorrelationData();
//
//        correlationData.setId(message_id);
//
//        return correlationData;
//    }


}
