package com.xy.server.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate(
                (correlationData, ack, cause) -> {
                    if (ack) {
                        log.info("single消息成功发送到交换机，消息ID: {}",
                                correlationData != null ? correlationData.getId() : null);
                    } else {
                        log.error("single消息发送到交换机失败，原因: {}", cause);
                    }
                },
                returnedMessage -> log.warn("RabbitMQ退回消息: 消息体：{}, 码:{}, 原因:{}, 交换机:{}, 路由键:{}",
                        returnedMessage.getMessage(), returnedMessage.getReplyCode(),
                        returnedMessage.getReplyText(), returnedMessage.getExchange(), returnedMessage.getRoutingKey())
        );
    }

    @Override
    @Transactional
    public Result send(IMSingleMessageDto dto) {
        try {
            // 消息id
            String messageId = IdUtil.getSnowflake().nextIdStr();
            // 消息时间
            Long messageTime = DateTimeUtils.getUTCDateTime();
            dto.setMessageId(messageId)
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.code());

            // 构建私信消息对象并异步插入
            ImPrivateMessagePo messagePo = new ImPrivateMessagePo();
            BeanUtils.copyProperties(dto, messagePo);
            insertImPrivateMessageAsync(messagePo);

            // 异步更新会话信息
            setChatAsync(dto.getFromId(), dto.getToId(), messageTime);

            // 通过redis查找接收者长连接信息
            Object redisObj = redisUtil.get(IMUSERPREFIX + dto.getToId());

            // 判断长连接信息是否为空，不为空则发送消息到mq
            if (ObjectUtil.isNotEmpty(redisObj)) {
                IMRegisterUserDto registerUser = JsonUtil.parseObject(redisObj, IMRegisterUserDto.class);
                String brokerId = registerUser.getBrokerId();
                IMessageWrap wrap = new IMessageWrap(IMessageType.SINGLE_MESSAGE.getCode(), dto);
                CorrelationData correlationData = new CorrelationData(messageId);
                rabbitTemplate.convertAndSend(EXCHANGENAME, ROUTERKEYPREFIX + brokerId,
                        JsonUtil.toJSONString(wrap), correlationData);
            } else {
                log.info("用户:{} 未登录", dto.getToId());
            }
            return Result.success(dto);
        } catch (Exception e) {

            log.error("发送单聊消息异常: {}", e.getMessage(), e);

            return Result.failed("发送消息失败");
        }
    }

    /**
     * 保存消息到数据库
     * @param messagePo
     */
    private void insertImPrivateMessageAsync(ImPrivateMessagePo messagePo) {
        CompletableFuture.runAsync(() -> {
            try {
                imPrivateMessageMapper.insert(messagePo);
            } catch (Exception e) {
                log.error("异步插入私信消息异常: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 保存会话
     * @param fromId
     * @param toId
     * @param messageTime
     */
    private void setChatAsync(String fromId, String toId, Long messageTime) {
        CompletableFuture.runAsync(() -> {
            try {
                setChat(fromId, toId, messageTime);
            } catch (Exception e) {
                log.error("异步更新会话异常: {}", e.getMessage(), e);
            }
        });
    }

    private void setChat(String fromId, String toId, Long messageTime) {
        createOrUpdateImChatSet(fromId, toId, messageTime);
        createOrUpdateImChatSet(toId, fromId, messageTime);
    }

    /**
     * 创建或更新会话
     * @param ownerId
     * @param toId
     * @param messageTime
     */
//    @LockTransactional(name = "'im-chat'+ #ownerId + ':createOrUpdateImChat'")
    private void createOrUpdateImChatSet(String ownerId, String toId, Long messageTime) {
        try {
            QueryWrapper<ImChatPo> query = new QueryWrapper<>();
            query.eq("owner_id", ownerId)
                    .eq("to_id", toId)
                    .eq("chat_type", IMessageType.SINGLE_MESSAGE.getCode());
            ImChatPo chatPo = imChatMapper.selectOne(query);

            if (ObjectUtil.isEmpty(chatPo)) {
                chatPo = new ImChatPo();
                chatPo.setChatId(UUID.randomUUID().toString())
                        .setOwnerId(ownerId)
                        .setToId(toId)
                        .setSequence(messageTime)
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setChatType(IMessageType.SINGLE_MESSAGE.getCode());
                imChatMapper.insert(chatPo);
            } else {
                chatPo.setSequence(messageTime);
                imChatMapper.updateById(chatPo);
            }

        } catch (Exception e) {
            log.error("创建或更新会话异常: ownerId={}, toId={}, messageTime={}, error={}",
                    ownerId, toId, messageTime, e.getMessage(), e);
        }
    }

}