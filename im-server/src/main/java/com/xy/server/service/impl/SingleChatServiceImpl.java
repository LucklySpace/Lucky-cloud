package com.xy.server.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.imcore.enums.IMStatus;
import com.xy.imcore.enums.IMessageReadStatus;
import com.xy.imcore.enums.IMessageSendStatus;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.imcore.model.IMessageWrap;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.mapper.ImChatMapper;
import com.xy.server.mapper.ImPrivateMessageMapper;
import com.xy.server.model.ImChat;
import com.xy.server.model.ImPrivateMessage;
import com.xy.server.response.Result;
import com.xy.server.service.SingleChatService;
import com.xy.server.utils.DateTimeUtils;
import com.xy.server.utils.JsonUtil;
import com.xy.server.utils.RedisUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.xy.imcore.constants.Constant.*;


@Slf4j
@Service
public class SingleChatServiceImpl implements SingleChatService {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ImPrivateMessageMapper imPrivateMessageMapper;

    @Resource
    private ImChatMapper imChatMapper;

    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;

    private RabbitTemplate rabbitTemplate;

    /**
     * 不同的消息实现不同的confirm
     * 保证消息可靠性
     */
    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate((correlationData, ack, cause) -> {
            if (ack) {
                log.info("single消息成功发送到交换机，消息ID: {}", correlationData != null ? correlationData.getId() : null);
            } else {
                log.error("single消息发送到交换机失败，原因: {}", cause);
                // 可以在此处做消息的重发或其他处理
            }
        }, (returnedMessage) -> {
            log.info("RabbitMQ-single交换机到队列退回:::return callback 消息体：{},应答码:{},原因:{},交换机:{},路由键:{}",
                    returnedMessage.getMessage(), returnedMessage.getReplyCode(), returnedMessage.getReplyText(),
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        });
    }


    @Override
    @Transactional
    public Result send(IMSingleMessageDto IMSingleMessageDto) {
        // 消息id
        String message_id = IdUtil.getSnowflake().nextIdStr();
        // 消息时间,使用utc时间
        long message_time =DateTimeUtils.getUTCDateTime();

        IMSingleMessageDto.setMessage_id(message_id);

        IMSingleMessageDto.setMessage_time(message_time);

        IMSingleMessageDto.setRead_status(IMessageReadStatus.UNREAD.code());

        ImPrivateMessage imPrivateMessage = new ImPrivateMessage();

        BeanUtils.copyProperties(IMSingleMessageDto, imPrivateMessage);

        imPrivateMessage.setRead_status(IMessageReadStatus.UNREAD.code());

        // 异步插入私信消息
        insertImPrivateMessageAsync(imPrivateMessage);

        // 异步处理会话
        setChatAsync(IMSingleMessageDto.getFrom_id(), IMSingleMessageDto.getTo_id(), message_time);

        // 通过redis获取用户连接netty的机器码
        Object redisObj = redisUtil.get(IMUSERPREFIX + IMSingleMessageDto.getTo_id());

        if (ObjectUtil.isNotEmpty(redisObj)) {

            IMRegisterUserDto IMRegisterUserDto = JsonUtil.parseObject(redisObj, new TypeReference<IMRegisterUserDto>() {});

            // 获取长连接机器码
            String broker_id = IMRegisterUserDto.getBroker_id();

            // 对发送消息进行包装
            IMessageWrap IMessageWrap = new IMessageWrap(IMessageType.SINGLE_MESSAGE.getCode(), IMSingleMessageDto);

            // 创建 CorrelationData，并设置消息ID
            CorrelationData correlationData = new CorrelationData(message_id);

            // 发送到消息队列
            rabbitTemplate.convertAndSend(EXCHANGENAME, ROUTERKEYPREFIX + broker_id, JsonUtil.toJSONString(IMessageWrap),correlationData);

        } else {
            log.info("用户:{} 未登录", IMSingleMessageDto.getTo_id());
        }

        return Result.success(IMSingleMessageDto);
    }


    protected void insertImPrivateMessageAsync(ImPrivateMessage imPrivateMessage) {
        log.info("接收人:{}  发送人:{}  消息内容:{}", imPrivateMessage.getTo_id(), imPrivateMessage.getFrom_id(), imPrivateMessage.getMessage_body());
        CompletableFuture.runAsync(() -> {
            imPrivateMessageMapper.insert(imPrivateMessage);
        });
    }

    private void setChatAsync(String from_id, String to_id, long message_time) {
        CompletableFuture.runAsync(() -> {
            setChat(from_id, to_id, message_time);
        });
    }


    public void setChat(String from_id, String to_id, long message_time) {
        createOrUpdateImChatSet(from_id, to_id, message_time);
        createOrUpdateImChatSet(to_id, from_id, message_time);
    }


    protected void createOrUpdateImChatSet(String owner_id, String to_id, long message_time) {

        QueryWrapper<ImChat> chatQuery = new QueryWrapper<>();

        // 查询会话是否存在
        chatQuery.eq("owner_id", owner_id);
        chatQuery.eq("to_id", to_id);
        chatQuery.eq("chat_type", IMessageType.SINGLE_MESSAGE.getCode());

        ImChat imChat = imChatMapper.selectOne(chatQuery);

        if (ObjectUtil.isEmpty(imChat)) {
            imChat = new ImChat();
            String id = UUID.randomUUID().toString();

            imChat.setChat_id(id)
                    .setOwner_id(owner_id)
                    .setTo_id(to_id)
                    .setSequence(message_time)
                    .setIs_mute(IMStatus.NO.getCode())
                    .setIs_top(IMStatus.NO.getCode())
                    .setChat_type(IMessageType.SINGLE_MESSAGE.getCode());

            imChatMapper.insert(imChat);
        } else {
            imChat.setSequence(message_time);
            imChatMapper.updateById(imChat);
        }
    }

}
