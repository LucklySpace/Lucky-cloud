package com.xy.server.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.imcore.enums.IMStatus;
import com.xy.imcore.enums.IMessageReadStatus;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.imcore.model.IMessageWrap;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.domain.po.ImChatPo;
import com.xy.server.domain.po.ImPrivateMessagePo;
import com.xy.server.mapper.ImChatMapper;
import com.xy.server.mapper.ImPrivateMessageMapper;
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
    public Result send(IMSingleMessageDto imSingleMessageDto) {
        // 消息id
        String messageId = IdUtil.getSnowflake().nextIdStr();
        // 消息时间,使用utc时间
        Long messageTime =DateTimeUtils.getUTCDateTime();

        imSingleMessageDto.setMessageId(messageId);

        imSingleMessageDto.setMessageTime(messageTime);

        imSingleMessageDto.setReadStatus(IMessageReadStatus.UNREAD.code());

        ImPrivateMessagePo imPrivateMessagePo = new ImPrivateMessagePo();

        BeanUtils.copyProperties(imSingleMessageDto, imPrivateMessagePo);

        // 异步插入私信消息
        insertImPrivateMessageAsync(imPrivateMessagePo);

        // 异步处理会话
        setChatAsync(imSingleMessageDto.getFromId(), imSingleMessageDto.getToId(), messageTime);

        // 通过redis获取用户连接netty的机器码
        Object redisObj = redisUtil.get(IMUSERPREFIX + imSingleMessageDto.getToId());

        if (ObjectUtil.isNotEmpty(redisObj)) {

            IMRegisterUserDto IMRegisterUserDto = JsonUtil.parseObject(redisObj, IMRegisterUserDto.class);

            // 获取长连接机器码
            String broker_id = IMRegisterUserDto.getBroker_id();

            // 对发送消息进行包装
            IMessageWrap IMessageWrap = new IMessageWrap(IMessageType.SINGLE_MESSAGE.getCode(), imSingleMessageDto);

            // 创建 CorrelationData，并设置消息ID
            CorrelationData correlationData = new CorrelationData(messageId);

            // 发送到消息队列
            rabbitTemplate.convertAndSend(EXCHANGENAME, ROUTERKEYPREFIX + broker_id, JsonUtil.toJSONString(IMessageWrap),correlationData);

        } else {
            log.info("用户:{} 未登录", imSingleMessageDto.getToId());
        }

        return Result.success(imSingleMessageDto);
    }


    protected void insertImPrivateMessageAsync(ImPrivateMessagePo imPrivateMessagePo) {
        log.info("接收人:{}  发送人:{}  消息内容:{}", imPrivateMessagePo.getToId(), imPrivateMessagePo.getFromId(), imPrivateMessagePo.getMessageBody());
        CompletableFuture.runAsync(() -> {
            imPrivateMessageMapper.insert(imPrivateMessagePo);
        });
    }

    private void setChatAsync(String fromId, String toId, Long messageTime) {
        CompletableFuture.runAsync(() -> {
            setChat(fromId, toId, messageTime);
        });
    }


    public void setChat(String fromId, String toId, Long messageTime) {
        createOrUpdateImChatSet(fromId, toId, messageTime);
        createOrUpdateImChatSet(toId, fromId, messageTime);
    }


    protected void createOrUpdateImChatSet(String ownerId, String toId, Long messageTime) {

        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();

        // 查询会话是否存在
        chatQuery.eq("owner_id", ownerId);
        chatQuery.eq("to_id", toId);
        chatQuery.eq("chat_type", IMessageType.SINGLE_MESSAGE.getCode());

        ImChatPo imChatPO = imChatMapper.selectOne(chatQuery);

        if (ObjectUtil.isEmpty(imChatPO)) {
            imChatPO = new ImChatPo();
            String id = UUID.randomUUID().toString();

            imChatPO.setChatId(id)
                    .setOwnerId(ownerId)
                    .setToId(toId)
                    .setSequence(messageTime)
                    .setIsMute(IMStatus.NO.getCode())
                    .setIsTop(IMStatus.NO.getCode())
                    .setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            imChatMapper.insert(imChatPO);
        } else {
            imChatPO.setSequence(messageTime);
            imChatMapper.updateById(imChatPO);
        }
    }

}
