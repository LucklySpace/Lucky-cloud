package com.xy.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.xy.domain.dto.ChatDto;
import com.xy.domain.po.*;
import com.xy.core.enums.IMStatus;
import com.xy.core.enums.IMessageReadStatus;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.*;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.api.database.chat.ImChatFeign;
import com.xy.server.api.database.group.ImGroupFeign;
import com.xy.server.api.database.message.ImMessageFeign;
import com.xy.server.api.id.IdGeneratorConstant;
import com.xy.server.api.id.ImIdGeneratorFeign;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.service.MessageService;
import com.xy.server.utils.JsonUtil;
import com.xy.server.utils.RedisUtil;
import com.xy.utils.DateTimeUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.xy.core.constants.IMConstant.*;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private ImMessageFeign imMessageFeign;

    @Resource
    private ImChatFeign imChatFeign;

    @Resource
    private ImIdGeneratorFeign imIdGeneratorFeign;

    @Resource
    private ImGroupFeign imGroupFeign;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;

    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate(
                (correlationData, ack, cause) -> {
                    if (ack) {
                        log.info("消息成功发送到交换机，消息ID: {}",
                                correlationData != null ? correlationData.getId() : null);
                    } else {
                        log.error("消息发送到交换机失败，原因: {}", cause);
                    }
                },
                returnedMessage -> {
                    log.info("\n确认消息送到队列(Queue)结果：");
                    log.info("发生消息：{}", returnedMessage.getMessage());
                    log.info("回应码：{}", returnedMessage.getReplyCode());
                    log.info("回应信息：{}", returnedMessage.getReplyText());
                    log.info("交换机：{}", returnedMessage.getExchange());
                    log.info("路由键：{}", returnedMessage.getRoutingKey());
                }
        );
    }

    /**
     * 发送私聊消息
     */
    @Override
    //@Transactional
    public Result<?> sendPrivateMessage(IMPrivateMessage dto) {
        try {

            // 消息id
            Long messageId = imIdGeneratorFeign.getId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id, Long.class);

            // 消息时间
            Long messageTime = DateTimeUtil.getCurrentUTCTimestamp();
            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            // 插入消息
            ImPrivateMessagePo messagePo = new ImPrivateMessagePo()
                    .setDelFlag(IMStatus.YES.getCode());

            BeanUtils.copyProperties(dto, messagePo);

            insertImPrivateMessageAsync(messagePo);

            // 更新会话信息
            setChatAsync(dto.getFromId(), dto.getToId(), messageTime);

            // 通过redis查找接收者长连接信息
            Object redisObj = redisUtil.get(USER_CACHE_PREFIX + dto.getToId());

            ResultCode messageRes;

            // 判断长连接信息是否为空，不为空则发送消息到mq
            if (ObjectUtil.isNotEmpty(redisObj)) {

                //序列化对象
                IMRegisterUser registerUser = JsonUtil.parseObject(redisObj, IMRegisterUser.class);

                // 获取机器码
                String brokerId = registerUser.getBrokerId();

                IMessageWrap<Object> imPrivateMessageIMessageWrap = new IMessageWrap<>().setCode(IMessageType.SINGLE_MESSAGE.getCode()).setData(dto).setIds(List.of(dto.getToId()));

                // 发送消息到 mq 消息队列
                rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME,  brokerId,
                        Objects.requireNonNull(JsonUtil.toJSONString(imPrivateMessageIMessageWrap)), new CorrelationData(messageId.toString()));

                messageRes = ResultCode.SUCCESS;

                log.info("单聊消息发送成功 发送用户:{} 接收用户:{}", dto.getFromId(), dto.getToId());
            } else {
                // 对方不在线
                messageRes = ResultCode.USER_OFFLINE;

                log.info("单聊消息未发送   发送用户:{}  接收用户:{} 未登录", dto.getFromId(), dto.getToId());
            }

            return Result.success(messageRes.getMessage(), dto);

        } catch (Exception e) {

            log.error("单聊消息发送异常: {}", e.getMessage(), e);

            return Result.failed("发送消息失败");
        }
    }

    /**
     * 保存私聊消息到数据库
     *
     * @param messagePo 私聊消息
     */
    private void insertImPrivateMessageAsync(ImPrivateMessagePo messagePo) {
        CompletableFuture.runAsync(() -> {
            try {
                if (imMessageFeign.privateMessageInsert(messagePo)) {

                    log.info("保存私聊消息成功 消息id:{} 发送人id:{} 接收人:{} 消息发送时间:{}", messagePo.getMessageId(), messagePo.getFromId(), messagePo.getToId(), messagePo.getMessageTime());
                } else {

                    log.error("保存私聊消息失败 消息id:{} 发送人id:{} 接收人:{} 消息发送时间:{}", messagePo.getMessageId(), messagePo.getFromId(), messagePo.getToId(), messagePo.getMessageTime());
                }
            } catch (Exception e) {

                log.error("保存私聊消息异常, 消息id:{} 发送人id:{} 接收人:{} 消息发送时间:{}", messagePo.getMessageId(), messagePo.getFromId(), messagePo.getToId(), messagePo.getMessageTime(), e);
            }
        });
    }

    /**
     * 保存或更新会话
     *
     * @param fromId      发送人
     * @param toId        接收人
     * @param messageTime 消息时间
     */
    private void setChatAsync(String fromId, String toId, Long messageTime) {
        CompletableFuture.runAsync(() -> {
            try {
                createOrUpdateImChatSet(fromId, toId, messageTime);
                createOrUpdateImChatSet(toId, fromId, messageTime);
            } catch (Exception e) {

                log.error("异步更新会话异常: {}", e.getMessage(), e);
            }
        });
    }


    /**
     * 创建或更新会话
     *
     * @param ownerId     所属人
     * @param toId        会话对象
     * @param messageTime 消息时间
     */
    private void createOrUpdateImChatSet(String ownerId, String toId, Long messageTime) {
        try {

            ImChatPo chatPo = imChatFeign.getOne(ownerId, toId, IMessageType.SINGLE_MESSAGE.getCode());

            if (ObjectUtil.isEmpty(chatPo)) {
                chatPo = new ImChatPo();

                // 会话id
                String chatId = imIdGeneratorFeign.getId(IdGeneratorConstant.uuid, IdGeneratorConstant.chat_id, String.class);
                //String chatId = UUID.randomUUID().toString();

                chatPo.setChatId(chatId)
                        .setOwnerId(ownerId)
                        .setToId(toId)
                        .setSequence(messageTime)
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setDelFlag(IMStatus.YES.getCode())
                        .setChatType(IMessageType.SINGLE_MESSAGE.getCode());

                if (imChatFeign.insert(chatPo)) {
                    log.info("保存私聊会话成功 会话id:{} 归属人id:{} 会话对象:{} 最新会话时间:{}", chatId, ownerId, toId, messageTime);
                } else {
                    log.error("保存私聊会话失败 会话id:{} 归属人id:{} 会话对象:{} 最新会话时间:{}", chatId, ownerId, toId, messageTime);
                }

            } else {
                // 更新会话时间
                chatPo.setSequence(messageTime);

                if (imChatFeign.updateById(chatPo)) {
                    log.info("更新私聊会话成功 会话id:{} 归属人id:{} 会话对象:{} 最新会话时间:{}", chatPo.getChatId(), ownerId, toId, messageTime);
                } else {
                    log.error("更新私聊会话失败 会话id:{} 归属人id:{} 会话对象:{} 最新会话时间:{}", chatPo.getChatId(), ownerId, toId, messageTime);
                }
            }

        } catch (Exception e) {
            log.error("创建或更新会话异常: ownerId={}, toId={}, messageTime={}, error={}",
                    ownerId, toId, messageTime, e.getMessage(), e);
        }
    }


    /**
     * 发送群聊消息
     *
     * @param imGroupMessage 群消息对象
     * @return 发送结果
     */
    @Override
    public Result<?> sendGroupMessage(IMGroupMessage imGroupMessage) {
        try {
            // 生成消息id
            //String messageId = IdUtil.getSnowflake().nextIdStr();
            Long messageId = imIdGeneratorFeign.getId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id, Long.class);

            // 获取群聊id
            String groupId = imGroupMessage.getGroupId();

            Long messageTime = DateTimeUtil.getCurrentUTCTimestamp();

            imGroupMessage.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setSequence(messageTime);

            // 异步插入群消息
            insertImGroupMessageAsync(imGroupMessage);

            // 获取群成员列表
            List<ImGroupMemberPo> imGroupMemberPos = imGroupFeign.getGroupMemberList(groupId);

            if (CollectionUtil.isEmpty(imGroupMemberPos)) {
                log.warn("群:{} 没有成员，无法发送消息", groupId);
                return Result.failed("群聊成员为空，无法发送消息");
            }

            // 过滤掉发送者，获取接收者ID列表
            List<String> toList = imGroupMemberPos.parallelStream()
                    .filter(member -> !member.getMemberId().equals(imGroupMessage.getFromId()))
                    .map(member -> USER_CACHE_PREFIX + member.getMemberId())
                    .collect(Collectors.toList());

            // 异步设置消息读取状态
            setReadStatusAsync(String.valueOf(messageId), groupId, imGroupMemberPos);

            // 更新会话信息
            setChatAsync(groupId, messageTime, imGroupMemberPos);

            // 批量获取用户的长连接信息
            List<Object> userObjList = redisUtil.batchGet(toList);
            Map<String, List<String>> brokerMap = new HashMap<>();

            // 根据长连接的  brokerId 对用户进行分类汇总
            for (Object redisObj : userObjList) {
                if (ObjectUtil.isNotEmpty(redisObj)) {

                    IMRegisterUser userDto = JsonUtil.parseObject(redisObj, IMRegisterUser.class);

                    // 根据 brokerId 对用户进行分类
                    brokerMap.computeIfAbsent(userDto.getBrokerId(), k -> new ArrayList<>()).add(userDto.getUserId());
                }
            }

            // 分发消息到不同的 broker
            for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
                // 机器码
                String brokerId = entry.getKey();
                // 群聊在线用户
                imGroupMessage.setToList(entry.getValue());

                IMessageWrap<Object> groupMessageIMessageWrap = new IMessageWrap<>().setCode(IMessageType.GROUP_MESSAGE.getCode()).setData(imGroupMessage).setIds(entry.getValue());

                // 发送消息到 mq
                rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME,  brokerId,
                        Objects.requireNonNull(JsonUtil.toJSONString(groupMessageIMessageWrap)), new CorrelationData(messageId.toString()));
            }

            return Result.success(imGroupMessage);

        } catch (Exception e) {
            log.error("群消息发送失败, 群ID: {}, 发送者: {}, 错误: {}",
                    imGroupMessage.getGroupId(), imGroupMessage.getFromId(), e.getMessage(), e);
            return Result.failed("发送群消息失败");
        }
    }

    /**
     * 异步插入群消息
     */
    private void insertImGroupMessageAsync(IMGroupMessage imGroupMessage) {
        CompletableFuture.runAsync(() -> {
            try {

                ImGroupMessagePo imGroupMessagePo = new ImGroupMessagePo()
                        .setDelFlag(IMStatus.YES.getCode());

                BeanUtils.copyProperties(imGroupMessage, imGroupMessagePo);

                // 保存群消息
                if (imMessageFeign.groupMessageInsert(imGroupMessagePo)) {

                    log.info("群消息保存成功, 群ID: {}, 发送者: {}", imGroupMessagePo.getGroupId(), imGroupMessagePo.getFromId());
                } else {

                    log.error("群消息保存失败, 群ID: {}, 发送者: {}", imGroupMessagePo.getGroupId(), imGroupMessagePo.getFromId());
                }
            } catch (Exception e) {

                log.error("异步插入群消息失败: {}", e.getMessage(), e);
            }
        });
    }


    /**
     * 异步更新会话信息
     */
    private void setChatAsync(String groupId, Long messageTime, List<ImGroupMemberPo> imGroupMemberPos) {
        CompletableFuture.runAsync(() -> setGroupChat(groupId, messageTime, imGroupMemberPos));
    }


    /**
     * 更新群聊会话信息
     *
     * @param groupId          群id
     * @param messageTime      时间
     * @param imGroupMemberPos 群成员
     */
    public void setGroupChat(String groupId, Long messageTime, List<ImGroupMemberPo> imGroupMemberPos) {
        try {
            for (ImGroupMemberPo member : imGroupMemberPos) {

                ImChatPo chatPo = imChatFeign.getOne(member.getMemberId(), groupId, IMessageType.GROUP_MESSAGE.getCode());

                String memberId = member.getMemberId();

                if (ObjectUtil.isEmpty(chatPo)) {
                    chatPo = new ImChatPo();

                    String chatId = UUID.randomUUID().toString();

                    chatPo.setChatId(chatId)
                            .setOwnerId(memberId)
                            .setToId(groupId)
                            .setSequence(messageTime)
                            .setIsMute(IMStatus.NO.getCode())
                            .setIsTop(IMStatus.NO.getCode())
                            .setDelFlag(IMStatus.YES.getCode())
                            .setChatType(IMessageType.GROUP_MESSAGE.getCode());

                    if (imChatFeign.insert(chatPo)) {

                        log.info("保存群聊会话成功 会话id:{} 归属人id:{} 群id:{} 最新会话时间:{}", chatId, memberId, groupId, messageTime);
                    } else {

                        log.error("保存群聊会话失败 会话id:{} 归属人id:{} 群id:{} 最新会话时间:{}", chatId, memberId, groupId, messageTime);
                    }

                } else {
                    // 更新会话时间
                    chatPo.setSequence(messageTime);

                    if (imChatFeign.updateById(chatPo)) {

                        log.info("更新群聊会话成功 会话id:{} 归属人id:{} 群id:{} 最新会话时间:{}", chatPo.getChatId(), memberId, groupId, messageTime);
                    } else {

                        log.error("更新群聊会话失败 会话id:{} 归属人id:{} 群id:{} 最新会话时间:{}", chatPo.getChatId(), memberId, groupId, messageTime);
                    }
                }
            }

            log.info("成功更新群会话信息, 群ID: {}", groupId);
        } catch (Exception e) {

            log.error("更新群会话信息失败, 群ID: {}, 错误: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * 异步设置消息阅读状态
     *
     * @param messageId        消息id
     * @param groupId          群id
     * @param imGroupMemberPos 群成员
     */
    private void setReadStatusAsync(String messageId, String groupId, List<ImGroupMemberPo> imGroupMemberPos) {
        CompletableFuture.runAsync(() -> setReadStatus(messageId, groupId, imGroupMemberPos));
    }

    /**
     * 设置群消息的阅读状态
     *
     * @param messageId        消息id
     * @param groupId          群id
     * @param imGroupMemberPos 群成员
     */
    public void setReadStatus(String messageId, String groupId, List<ImGroupMemberPo> imGroupMemberPos) {
        try {
            // 设置所有群消息未读
            List<ImGroupMessageStatusPo> groupReadStatusList = imGroupMemberPos.stream()
                    .map(member -> new ImGroupMessageStatusPo()
                            .setMessageId(messageId)
                            .setGroupId(groupId)
                            .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                            .setToId(member.getMemberId()))
                    .collect(Collectors.toList());

            // 插入数据库
            imMessageFeign.groupMessageStatusBatchInsert(groupReadStatusList);

            log.info("成功设置群消息阅读状态, 消息ID: {}", messageId);

        } catch (Exception e) {

            log.error("设置群消息阅读状态失败, 消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
        }
    }

    @Override
    public Result sendVideoMessage(IMVideoMessage videoMessage) {
        if (videoMessage == null || videoMessage.getToId() == null) {
            return Result.failed("参数无效，消息或接收方为空");
        }

        // 从 Redis 获取用户连接信息
        Object redisObj = redisUtil.get(USER_CACHE_PREFIX + videoMessage.getToId());
        if (ObjectUtil.isEmpty(redisObj)) {
            log.info("用户 [{}] 未登录，消息发送失败", videoMessage.getToId());
            return Result.failed("用户未登录");
        }

        try {
            // 解析用户连接信息
            IMRegisterUser targetUser = JsonUtil.parseObject(redisObj, IMRegisterUser.class);
            String brokerId = targetUser.getBrokerId();

            // 包装消息
            IMessageWrap<Object> wrapMsg = new IMessageWrap<>()
                    .setCode(IMessageType.VIDEO_MESSAGE.getCode())
                    .setData(videoMessage)
                    .setIds(List.of(videoMessage.getToId()));

            // 发送消息到 MQ
            rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME, brokerId, JsonUtil.toJSONString(wrapMsg));

            log.info("视频消息发送成功，from={} → to={} via broker={}",
                    videoMessage.getFromId(), videoMessage.getToId(), brokerId);

            return Result.success("消息发送成功");
        } catch (Exception e) {
            log.error("发送视频消息异常，toId={}", videoMessage.getToId(), e);
            return Result.failed("消息发送异常");
        }
    }

    @Override
    public Map<Integer, Object> list(ChatDto chatDto) {

        String userId = chatDto.getFromId();

        Long sequence = chatDto.getSequence();

        CompletableFuture<List<ImPrivateMessagePo>> singleMessageFuture = CompletableFuture.supplyAsync(() -> imMessageFeign.getPrivateMessageList(userId, sequence));
        CompletableFuture<List<ImGroupMessagePo>> groupMessageFuture = CompletableFuture.supplyAsync(() -> imMessageFeign.getGroupMessageList(userId, sequence));

        Map<Integer, Object> map = new HashMap<>();

        try {
            List<ImPrivateMessagePo> IMPrivateMessageDtoList = singleMessageFuture.get();
            if (ObjectUtil.isNotEmpty(IMPrivateMessageDtoList)) {
                map.put(IMessageType.SINGLE_MESSAGE.getCode(), IMPrivateMessageDtoList);
            }

            List<ImGroupMessagePo> IMGroupMessagePoDtoList = groupMessageFuture.get();
            if (ObjectUtil.isNotEmpty(IMGroupMessagePoDtoList)) {
                map.put(IMessageType.GROUP_MESSAGE.getCode(), IMGroupMessagePoDtoList);
            }
        } catch (InterruptedException | ExecutionException e) {
            // 处理异常
        }

        return map;
    }

}
