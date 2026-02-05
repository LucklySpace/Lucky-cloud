package com.xy.lucky.server.service.impl;

import com.xy.lucky.api.ImIdDubboService;
import com.xy.lucky.api.chat.ImChatDubboService;
import com.xy.lucky.api.group.ImGroupMemberDubboService;
import com.xy.lucky.api.message.ImGroupMessageDubboService;
import com.xy.lucky.api.message.ImSingleMessageDubboService;
import com.xy.lucky.api.outbox.IMOutboxDubboService;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.*;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.mq.rabbit.core.RabbitTemplateFactory;
import com.xy.lucky.server.common.LockExecutor;
import com.xy.lucky.server.config.IdGeneratorConstant;
import com.xy.lucky.server.domain.dto.ChatDto;
import com.xy.lucky.server.domain.mapper.MessageBeanMapper;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.MessageService;
import com.xy.lucky.server.service.MuteService;
import com.xy.lucky.server.utils.RedisUtil;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.json.JacksonUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.xy.lucky.core.constants.IMConstant.MQ_EXCHANGE_NAME;
import static com.xy.lucky.core.constants.IMConstant.USER_CACHE_PREFIX;

/**
 * 消息服务实现
 *
 * @author xy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    /**
     * 分布式锁前缀
     */
    private static final String LOCK_PREFIX = "lock:message:";
    /**
     * 撤回消息时间限制（毫秒）
     */
    private static final long RECALL_TIMEOUT_MS = 2 * 60 * 1000L;

    /**
     * 消息 ID 到发件箱 ID 的映射
     */
    private final Map<String, Long> messageOutboxIdMap = new ConcurrentHashMap<>();
    private final LockExecutor lockExecutor;
    @DubboReference
    private ImChatDubboService chatDubboService;
    @DubboReference
    private ImGroupMemberDubboService groupMemberDubboService;
    @DubboReference
    private ImSingleMessageDubboService singleMessageDubboService;
    @DubboReference
    private ImGroupMessageDubboService groupMessageDubboService;
    @DubboReference
    private IMOutboxDubboService outboxDubboService;
    @DubboReference
    private ImIdDubboService idDubboService;
    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;

    @Resource
    private MuteService muteService;

    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    private RabbitTemplate rabbitTemplate;

    /**
     * 初始化 RabbitMQ 模板
     */
    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate(
                this::handleConfirmCallback,
                this::handleReturnCallback
        );
    }

    /**
     * 发送单聊消息
     *
     * @param dto 消息内容（已在 Controller 层校验）
     * @return 发送后的消息
     */
    @Override
    public IMSingleMessage sendSingleMessage(IMSingleMessage dto) {
        if (muteService.isMutedInPrivate(dto.getFromId(), dto.getToId())) {
            throw new MessageException("禁言中，无法发送消息");
        }
        String lockKey = LOCK_PREFIX + "single:" + dto.getFromId() + ":" + dto.getToId();
        return lockExecutor.execute(lockKey, () -> {
            Long messageId = generateLongId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id);
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            asyncPersistSingleMessage(dto, messageId, messageTime);
            sendToOnlineUser(dto, String.valueOf(messageId));

            log.info("发送单聊消息: from={}, to={}, messageId={}", dto.getFromId(), dto.getToId(), messageId);
            return dto;
        });
    }

    /**
     * 发送群聊消息
     *
     * @param dto 消息内容（已在 Controller 层校验）
     * @return 发送后的消息
     */
    @Override
    public IMGroupMessage sendGroupMessage(IMGroupMessage dto) {
        if (muteService.isMutedInGroup(dto.getGroupId(), dto.getFromId())) {
            throw new MessageException("禁言中，无法在群聊发送消息");
        }
        String lockKey = LOCK_PREFIX + "group:" + dto.getGroupId() + ":" + dto.getFromId();
        return lockExecutor.execute(lockKey, () -> {
            List<ImGroupMemberPo> members = groupMemberDubboService.queryList(dto.getGroupId());
            if (CollectionUtils.isEmpty(members)) {
                log.warn("群聊没有成员: groupId={}", dto.getGroupId());
                return dto;
            }

            Long messageId = generateLongId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id);
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            asyncPersistGroupMessage(dto, messageId, messageTime, members);
            sendToOnlineMembers(dto, members, String.valueOf(messageId), IMessageType.GROUP_MESSAGE);

            log.info("发送群聊消息: from={}, groupId={}, messageId={}", dto.getFromId(), dto.getGroupId(), messageId);
            return dto;
        });
    }

    /**
     * 发送群组操作
     *
     * @param dto 操作内容
     * @return 发送后的操作
     */
    @Override
    public void sendGroupAction(IMGroupAction dto) {

        String fromId = StringUtils.hasText(dto.getFromId()) ? dto.getFromId() : IMConstant.SYSTEM;

        dto.setFromId(fromId);

        String lockKey = LOCK_PREFIX + "groupAction:" + dto.getGroupId() + ":" + fromId;

        lockExecutor.execute(lockKey, () -> {
            List<ImGroupMemberPo> members = groupMemberDubboService.queryList(dto.getGroupId());
            if (CollectionUtils.isEmpty(members)) {
                log.warn("群组操作没有成员: groupId={}", dto.getGroupId());
                return;
            }
            Long messageId = IdUtils.snowflakeId();

            dto.setMessageId(String.valueOf(messageId));

            sendToOnlineMembers(dto, members, String.valueOf(messageId), IMessageType.GROUP_OPERATION);

            log.info("发送群组操作消息: from={}, groupId={}, messageId={}", dto.getFromId(), dto.getGroupId(), messageId);
        });
    }

    /**
     * 发送视频消息
     *
     * @param dto 视频消息内容（已在 Controller 层校验）
     */
    @Override
    public void sendVideoMessage(IMVideoMessage dto) {
        if (muteService.isMutedInPrivate(dto.getFromId(), dto.getToId())) {
            return;
        }
        String lockKey = LOCK_PREFIX + "video:" + dto.getFromId() + ":" + dto.getToId();
        lockExecutor.execute(lockKey, () -> {
            IMRegisterUser receiver = getOnlineUser(dto.getToId());
            if (receiver == null) {
                log.info("视频消息接收者不在线: from={}, to={}", dto.getFromId(), dto.getToId());
                return;
            }

            IMessageWrap<Object> wrapper = buildMessageWrapper(IMessageType.VIDEO_MESSAGE.getCode(), dto, List.of(dto.getToId()));
            publishToBroker(receiver.getBrokerId(), wrapper, null);
        });
    }

    /**
     * 撤回消息
     *
     * @param dto 撤回请求（已在 Controller 层校验）
     */
    @Override
    public void recallMessage(IMessageAction dto) {
        String lockKey = LOCK_PREFIX + "recall:" + dto.getMessageId();
        lockExecutor.execute(lockKey, () -> {
            // 单聊消息
            if (Objects.equals(dto.getMessageType(), IMessageType.SINGLE_MESSAGE.getCode())) {
                ImSingleMessagePo singleMsg = singleMessageDubboService.queryOne(dto.getMessageId());
                if (singleMsg != null) {
                    recallSingleMessage(singleMsg, dto);
                    return;
                }
            }

            // 群聊消息
            if (Objects.equals(dto.getMessageType(), IMessageType.GROUP_MESSAGE.getCode())) {
                ImGroupMessagePo groupMsg = groupMessageDubboService.queryOne(dto.getMessageId());
                if (groupMsg != null) {
                    recallGroupMessage(groupMsg, dto);
                    return;
                }
            }

            throw new MessageException("消息或类型不存在");
        });
    }

    /**
     * 查询消息列表
     *
     * @param dto 查询条件（已在 Controller 层校验）
     * @return 消息列表（按类型分组）
     */
    @Override
    public Map<Integer, Object> list(ChatDto dto) {
        Map<Integer, Object> result = new HashMap<>();

        List<ImSingleMessagePo> singleMessages = singleMessageDubboService.queryList(dto.getFromId(), dto.getSequence());
        if (!CollectionUtils.isEmpty(singleMessages)) {
            result.put(IMessageType.SINGLE_MESSAGE.getCode(), singleMessages);
        }

        List<ImGroupMessagePo> groupMessages = groupMessageDubboService.queryList(dto.getFromId(), dto.getSequence());
        if (!CollectionUtils.isEmpty(groupMessages)) {
            result.put(IMessageType.GROUP_MESSAGE.getCode(), groupMessages);
        }

        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 异步持久化单聊消息
     */
    private void asyncPersistSingleMessage(IMSingleMessage dto, Long messageId, Long messageTime) {
        asyncTaskExecutor.execute(() -> {
            try {
                ImSingleMessagePo po = MessageBeanMapper.INSTANCE.toImSingleMessagePo(dto);
                po.setDelFlag(IMStatus.YES.getCode());
                saveSingleMessage(po);

                createOrUpdateChat(dto.getFromId(), dto.getToId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
                createOrUpdateChat(dto.getToId(), dto.getFromId(), messageTime, IMessageType.SINGLE_MESSAGE.getCode());
            } catch (Exception e) {
                log.error("异步持久化单聊消息失败: messageId={}", messageId, e);
            }
        });
    }

    /**
     * 异步持久化群聊消息
     */
    private void asyncPersistGroupMessage(IMGroupMessage dto, Long messageId, Long messageTime, List<ImGroupMemberPo> members) {
        asyncTaskExecutor.execute(() -> {
            try {
                ImGroupMessagePo po = MessageBeanMapper.INSTANCE.toImGroupMessagePo(dto);
                po.setDelFlag(IMStatus.YES.getCode());
                saveGroupMessage(po);

                setGroupMessageReadStatus(String.valueOf(messageId), dto.getGroupId(), members);

                for (ImGroupMemberPo member : members) {
                    createOrUpdateChat(member.getMemberId(), dto.getGroupId(), messageTime, IMessageType.GROUP_MESSAGE.getCode());
                }
            } catch (Exception e) {
                log.error("异步持久化群聊消息失败: messageId={}", messageId, e);
            }
        });
    }

    /**
     * 发送消息给在线用户
     */
    private void sendToOnlineUser(IMSingleMessage dto, String messageId) {
        IMRegisterUser receiver = getOnlineUser(dto.getToId());
        if (receiver == null) {
            log.debug("接收者不在线: toId={}", dto.getToId());
            return;
        }

        IMessageWrap<Object> wrapper = buildMessageWrapper(IMessageType.SINGLE_MESSAGE.getCode(), dto, List.of(dto.getToId()));
        publishToBroker(receiver.getBrokerId(), wrapper, messageId);
    }

    /**
     * 发送消息给在线群成员
     */
    private void sendToOnlineMembers(IMessage dto, List<ImGroupMemberPo> members, String messageId, IMessageType messageType) {
        List<String> memberKeys = members.stream()
                .map(m -> USER_CACHE_PREFIX + m.getMemberId())
                .toList();

        List<Object> userObjs = redisUtil.batchGet(memberKeys);
        if (CollectionUtils.isEmpty(userObjs)) {
            return;
        }

        Map<String, List<String>> brokerUserMap = new HashMap<>();
        for (Object obj : userObjs) {
            if (obj == null) continue;

            IMRegisterUser user = JacksonUtils.parseObject(obj, IMRegisterUser.class);
            if (user == null || user.getUserId() == null || user.getBrokerId() == null) continue;
            //if (user.getUserId().equals(dto.getFromId())) continue;

            brokerUserMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
        }

        brokerUserMap.forEach((brokerId, userIds) -> {
            IMessageWrap<Object> wrapper = buildMessageWrapper(messageType.getCode(), dto, userIds);
            publishToBroker(brokerId, wrapper, messageId);
        });
    }

    /**
     * 撤回单聊消息
     */
    private void recallSingleMessage(ImSingleMessagePo msg, IMessageAction dto) {

        long recallTime = DateTimeUtils.getCurrentUTCTimestamp();

        // 验证撤回权限和时间
        validateRecallPermission(msg.getFromId(), dto.getFromId(), recallTime, msg.getMessageTime());

        // 构建撤回消息体
        IMessage.RecallMessageBody recallBody = buildRecallMessageBody(
                dto.getMessageId(), dto.getFromId(), recallTime,
                msg.getToId(), IMessageType.SINGLE_MESSAGE.getCode());

        // 更新数据库中的消息状态
        msg.setMessageContentType(IMessageContentType.RECALL_MESSAGE.getCode());
        msg.setMessageBody(recallBody);
        singleMessageDubboService.modify(msg);

        // 发送撤回通知给接收者
        IMessageAction recallAction = buildRecallAction(dto.getFromId(), msg.getToId(), null,
                dto.getMessageId(), recallTime, recallBody);

        sendRecallToUser(msg.getToId(), recallAction);

        log.info("撤回单聊消息: messageId={}, fromId={}, toId={}", dto.getMessageId(), dto.getFromId(), msg.getToId());
    }

    /**
     * 撤回群聊消息
     */
    private void recallGroupMessage(ImGroupMessagePo msg, IMessageAction dto) {

        long recallTime = DateTimeUtils.getCurrentUTCTimestamp();

        // 验证撤回权限和时间
        validateRecallPermission(msg.getFromId(), dto.getFromId(), recallTime, msg.getMessageTime());

        // 构建撤回消息体
        IMessage.RecallMessageBody recallBody = buildRecallMessageBody(
                dto.getMessageId(), dto.getFromId(), recallTime,
                msg.getGroupId(), IMessageType.GROUP_MESSAGE.getCode());

        // 更新数据库中的消息状态
        msg.setMessageContentType(IMessageContentType.RECALL_MESSAGE.getCode());
        msg.setMessageBody(recallBody);
        groupMessageDubboService.modify(msg);

        // 通知群成员
        IMessageAction recallAction = buildRecallAction(dto.getFromId(), null, msg.getGroupId(),
                dto.getMessageId(), recallTime, recallBody);
        sendRecallToGroupMembers(msg.getGroupId(), dto.getFromId(), recallAction);

        log.info("撤回群聊消息: messageId={}, fromId={}, groupId={}", dto.getMessageId(), dto.getFromId(), msg.getGroupId());
    }

    // ==================== 撤回消息辅助方法 ====================

    /**
     * 验证撤回权限和时间
     */
    private void validateRecallPermission(String messageFromId, String operatorId, Long now, Long messageTime) {
        if (!messageFromId.equals(operatorId)) {
            throw new MessageException("无权撤回他人消息");
        }

        if (now - messageTime > RECALL_TIMEOUT_MS) {
            throw new MessageException("发送时间超过2分钟，无法撤回");
        }
    }

    /**
     * 构建撤回消息体
     */
    private IMessage.RecallMessageBody buildRecallMessageBody(String messageId, String operatorId,
                                                              long recallTime, String chatId, Integer chatType) {
        return new IMessage.RecallMessageBody()
                .setMessageId(messageId)
                .setOperatorId(operatorId)
                .setRecallTime(recallTime)
                .setChatId(chatId)
                .setChatType(chatType);
    }

    /**
     * 构建撤回操作消息
     */
    private IMessageAction buildRecallAction(String fromId, String toId, String groupId,
                                             String messageId, long recallTime,
                                             IMessage.RecallMessageBody recallBody) {
        return IMessageAction.builder()
                .messageTempId(IdUtils.snowflakeIdStr())
                .fromId(fromId)
                .toId(toId)
                .groupId(groupId)
                .messageId(messageId)
                .messageContentType(IMessageContentType.RECALL_MESSAGE.getCode())
                .messageTime(recallTime)
                .messageBody(recallBody)
                .build();
    }

    /**
     * 发送撤回通知给单个用户
     */
    private void sendRecallToUser(String userId, IMessageAction recallAction) {
        IMRegisterUser receiver = getOnlineUser(userId);
        if (receiver != null && receiver.getBrokerId() != null) {
            IMessageWrap<Object> wrapper = buildMessageWrapper(
                    IMessageType.MESSAGE_OPERATION.getCode(), recallAction, List.of(userId));
            publishToBroker(receiver.getBrokerId(), wrapper, recallAction.getMessageId());
        }
    }

    /**
     * 发送撤回通知给群成员
     */
    private void sendRecallToGroupMembers(String groupId, String fromId, IMessageAction recallAction) {
        List<ImGroupMemberPo> members = groupMemberDubboService.queryList(groupId);
        if (CollectionUtils.isEmpty(members)) {
            return;
        }

        List<String> memberKeys = members.stream()
                .map(m -> USER_CACHE_PREFIX + m.getMemberId())
                .toList();

        List<Object> userObjs = redisUtil.batchGet(memberKeys);
        if (CollectionUtils.isEmpty(userObjs)) {
            return;
        }

        Map<String, List<String>> brokerUserMap = new HashMap<>();
        for (Object obj : userObjs) {
            if (obj instanceof IMRegisterUser user && user.getBrokerId() != null && user.getUserId() != null) {
                brokerUserMap.computeIfAbsent(user.getBrokerId(), k -> new ArrayList<>()).add(user.getUserId());
            }
        }

        brokerUserMap.forEach((brokerId, userIds) -> {
            IMessageWrap<Object> wrapper = buildMessageWrapper(
                    IMessageType.MESSAGE_OPERATION.getCode(), recallAction, userIds);
            publishToBroker(brokerId, wrapper, recallAction.getMessageId());
        });
    }

    // ==================== 辅助方法 ====================

    private IMRegisterUser getOnlineUser(String userId) {
        Object obj = redisUtil.get(USER_CACHE_PREFIX + userId);
        return obj instanceof IMRegisterUser ? (IMRegisterUser) obj : null;
    }

    private Long generateLongId(String type, String businessType) {
        return idDubboService.generateId(type, businessType).getLongId();
    }

    private String generateStringId(String type, String businessType) {
        return idDubboService.generateId(type, businessType).getStringId();
    }

    private IMessageWrap<Object> buildMessageWrapper(Integer code, Object data, List<String> ids) {
        return new IMessageWrap<>().setCode(code).setData(data).setIds(ids);
    }

    private void publishToBroker(String routingKey, IMessageWrap<Object> wrapper, String messageId) {
        String payload = JacksonUtils.toJSONString(wrapper);
        rabbitTemplate.convertAndSend(MQ_EXCHANGE_NAME, routingKey, payload, new CorrelationData(messageId));
    }

    private void saveSingleMessage(ImSingleMessagePo po) {
        if (!singleMessageDubboService.creat(po)) {
            log.error("保存单聊消息失败: messageId={}", po.getMessageId());
        }
    }

    private void saveGroupMessage(ImGroupMessagePo po) {
        if (!groupMessageDubboService.creat(po)) {
            log.error("保存群聊消息失败: messageId={}", po.getMessageId());
        }
    }

    private void createOrUpdateChat(String ownerId, String toId, Long messageTime, Integer chatType) {
        try {
            ImChatPo chatPo = chatDubboService.queryOne(ownerId, toId, chatType);
            if (chatPo == null) {
                chatPo = new ImChatPo()
                        .setChatId(generateStringId(IdGeneratorConstant.uuid, IdGeneratorConstant.chat_id))
                        .setOwnerId(ownerId)
                        .setToId(toId)
                        .setSequence(messageTime)
                        .setIsMute(IMStatus.NO.getCode())
                        .setIsTop(IMStatus.NO.getCode())
                        .setChatType(chatType);
                chatDubboService.creat(chatPo);
            } else {
                chatPo.setSequence(messageTime);
                chatDubboService.modify(chatPo);
            }
        } catch (Exception e) {
            log.error("创建/更新会话失败: ownerId={}, toId={}", ownerId, toId, e);
        }
    }

    private void setGroupMessageReadStatus(String messageId, String groupId, List<ImGroupMemberPo> members) {
        try {
            List<ImGroupMessageStatusPo> statusList = members.stream()
                    .map(m -> new ImGroupMessageStatusPo()
                            .setMessageId(messageId)
                            .setGroupId(groupId)
                            .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                            .setToId(m.getMemberId()))
                    .collect(Collectors.toList());
            groupMessageDubboService.creatBatch(statusList);
        } catch (Exception e) {
            log.error("设置群消息读状态失败: messageId={}", messageId, e);
        }
    }

    private void handleConfirmCallback(CorrelationData correlationData, boolean ack, String cause) {
        String messageId = correlationData != null ? correlationData.getId() : null;
        if (ack) {
            log.debug("消息确认成功: messageId={}", messageId);
        } else {
            log.warn("消息确认失败: messageId={}, cause={}", messageId, cause);
        }
        updateOutboxStatus(messageId, ack);
    }

    private void handleReturnCallback(org.springframework.amqp.core.ReturnedMessage returned) {
        log.warn("消息被退回: exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        Object corr = returned.getMessage().getMessageProperties().getCorrelationId();
        String messageId = corr != null ? corr.toString() : null;
        updateOutboxStatus(messageId, false);
    }

    private void updateOutboxStatus(String messageId, boolean success) {
        if (messageId == null) return;
        Long outboxId = messageOutboxIdMap.remove(messageId);
        if (outboxId != null) {
            asyncTaskExecutor.execute(() -> {
                try {
                    Integer statusCode = success
                            ? IMOutboxStatus.SUCCESS.getCode()
                            : IMOutboxStatus.FAILED.getCode();
                    outboxDubboService.modifyStatus(outboxId, String.valueOf(statusCode), IMStatus.YES.getCode());
                } catch (Exception e) {
                    log.error("更新发件箱状态失败: outboxId={}", outboxId, e);
                }
            });
        }
    }
}
