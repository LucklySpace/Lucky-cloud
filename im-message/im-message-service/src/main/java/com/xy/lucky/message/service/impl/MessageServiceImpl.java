package com.xy.lucky.message.service.impl;

import com.xy.lucky.message.common.LockExecutor;
import com.xy.lucky.message.config.IdGeneratorConstant;
import com.xy.lucky.message.domain.dto.ChatDto;
import com.xy.lucky.message.domain.mapper.MessageBeanMapper;
import com.xy.lucky.message.exception.MessageException;
import com.xy.lucky.message.message.MessageLifecycleOrchestrator;
import com.xy.lucky.message.service.MessageService;
import com.xy.lucky.message.service.MuteService;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.*;
import com.xy.lucky.core.model.*;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.rpc.api.database.chat.ImChatDubboService;
import com.xy.lucky.rpc.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.rpc.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.rpc.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.rpc.api.leaf.ImIdDubboService;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
    private ImIdDubboService idDubboService;

    private final MuteService muteService;

    private final MessageBeanMapper messageBeanMapper;
    private final MessageLifecycleOrchestrator messageLifecycleOrchestrator;

    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

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
//        return lockExecutor.execute(lockKey, () -> {
            // 生成消息 ID
            Long messageId = generateLongId(IdGeneratorConstant.snowflake, IdGeneratorConstant.private_message_id);
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            // 填充消息
            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            // 持久化消息
            asyncPersistSingleMessage(dto, messageId, messageTime);

            // 消息投递
            messageLifecycleOrchestrator.dispatch(
                    IMessageType.SINGLE_MESSAGE.getCode(),
                    dto,
                    List.of(dto.getFromId(), dto.getToId()),
                    String.valueOf(messageId)
            );
            log.info("发送单聊消息: from={}, to={}, messageId={}", dto.getFromId(), dto.getToId(), messageId);
            return dto;
//        });
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

            // 获取群成员
            List<ImGroupMemberPo> members = groupMemberDubboService.queryList(dto.getGroupId());
            if (CollectionUtils.isEmpty(members)) {
                log.warn("群聊没有成员: groupId={}", dto.getGroupId());
                return dto;
            }

            // 生成消息 ID
            Long messageId = generateLongId(IdGeneratorConstant.snowflake, IdGeneratorConstant.group_message_id);
            Long messageTime = DateTimeUtils.getCurrentUTCTimestamp();

            // 填充消息
            dto.setMessageId(String.valueOf(messageId))
                    .setMessageTime(messageTime)
                    .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                    .setSequence(messageTime);

            // 持久化消息
            asyncPersistGroupMessage(dto, messageId, messageTime, members);

            // 过滤群成员ID
            List<String> targetUserIds = members.stream().map(ImGroupMemberPo::getMemberId).toList();

            // 群聊消息投递
            messageLifecycleOrchestrator.dispatch(
                    IMessageType.GROUP_MESSAGE.getCode(),
                    dto,
                    targetUserIds,
                    String.valueOf(messageId)
            );

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

            List<String> targetUserIds = members.stream().map(ImGroupMemberPo::getMemberId).toList();
            messageLifecycleOrchestrator.dispatch(
                    IMessageType.GROUP_OPERATION.getCode(),
                    dto,
                    targetUserIds,
                    String.valueOf(messageId)
            );

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
        lockExecutor.execute(lockKey, () -> messageLifecycleOrchestrator.dispatch(
                IMessageType.VIDEO_MESSAGE.getCode(),
                dto,
                List.of(dto.getToId()),
                IdUtils.snowflakeIdStr()
        ));
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
            if (Objects.equals(dto.getMessageType(), IMessageType.SINGLE_MESSAGE.getCode())) {
                ImSingleMessagePo singleMsg = Optional.ofNullable(singleMessageDubboService.queryOne(dto.getMessageId()))
                        .orElseThrow(() -> new MessageException("消息或类型不存在"));
                recallSingleMessage(singleMsg, dto);
                return;
            }

            if (Objects.equals(dto.getMessageType(), IMessageType.GROUP_MESSAGE.getCode())) {
                ImGroupMessagePo groupMsg = Optional.ofNullable(groupMessageDubboService.queryOne(dto.getMessageId()))
                        .orElseThrow(() -> new MessageException("消息或类型不存在"));
                recallGroupMessage(groupMsg, dto);
                return;
            }

            throw new MessageException("消息或类型不存在");
        });
    }

    /**
     * 接收客户端消息确认并同步状态。
     *
     * @param messageId 消息 ID
     * @param userId    确认用户 ID
     */
    @Override
    public void acknowledge(String messageId, String userId) {
        messageLifecycleOrchestrator.acknowledge(messageId, userId);
    }

    /**
     * 触发用户离线消息补发。
     *
     * @param userId 用户 ID
     */
    @Override
    public void replayOfflineMessages(String userId) {
        messageLifecycleOrchestrator.replayOfflineMessages(userId);
    }

    /**
     * 查询私聊消息列表
     *
     * @param dto 查询条件（已在 Controller 层校验）
     * @return 消息列表（按类型分组）
     */
    @Override
    public List<ImSingleMessagePo> singleList(ChatDto dto) {
        return singleMessageDubboService.queryList(dto.getFromId(), dto.getSequence());
    }

    /**
     * 查询群聊消息列表
     *
     * @param dto 查询条件（已在 Controller 层校验）
     * @return 消息列表（按类型分组）
     */
    @Override
    public List<ImGroupMessagePo> groupList(ChatDto dto) {
        return groupMessageDubboService.queryList(dto.getFromId(), dto.getSequence());
    }

    // ==================== 私有方法 ====================

    /**
     * 异步持久化单聊消息
     */
    private void asyncPersistSingleMessage(IMSingleMessage dto, Long messageId, Long messageTime) {
        asyncTaskExecutor.execute(() -> {
            try {
                ImSingleMessagePo po = messageBeanMapper.toImSingleMessagePo(dto);
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
                ImGroupMessagePo po = messageBeanMapper.toImGroupMessagePo(dto);
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
        sendRecallToGroupMembers(msg.getGroupId(), recallAction);

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
        messageLifecycleOrchestrator.dispatch(
                IMessageType.MESSAGE_OPERATION.getCode(),
                recallAction,
                List.of(userId),
                recallAction.getMessageId()
        );
    }

    /**
     * 发送撤回通知给群成员
     */
    private void sendRecallToGroupMembers(String groupId, IMessageAction recallAction) {
        List<ImGroupMemberPo> members = groupMemberDubboService.queryList(groupId);
        if (CollectionUtils.isEmpty(members)) {
            return;
        }
        List<String> targetUserIds = members.stream().map(ImGroupMemberPo::getMemberId).toList();
        messageLifecycleOrchestrator.dispatch(
                IMessageType.MESSAGE_OPERATION.getCode(),
                recallAction,
                targetUserIds,
                recallAction.getMessageId()
        );
    }

    // ==================== 辅助方法 ====================

    private Long generateLongId(String type, String businessType) {
        return idDubboService.generateId(type, businessType).getLongId();
    }

    private String generateStringId(String type, String businessType) {
        return idDubboService.generateId(type, businessType).getStringId();
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
            ImChatPo existing = chatDubboService.queryOne(ownerId, toId, chatType);
            Optional.ofNullable(existing)
                    .map(chatPo -> {
                        chatPo.setSequence(messageTime);
                        chatDubboService.modify(chatPo);
                        return chatPo;
                    })
                    .orElseGet(() -> {
                        ImChatPo chatPo = new ImChatPo()
                                .setChatId(generateStringId(IdGeneratorConstant.uuid, IdGeneratorConstant.chat_id))
                                .setOwnerId(ownerId)
                                .setToId(toId)
                                .setSequence(messageTime)
                                .setIsMute(IMStatus.NO.getCode())
                                .setIsTop(IMStatus.NO.getCode())
                                .setChatType(chatType);
                        chatDubboService.creat(chatPo);
                        return chatPo;
                    });
        } catch (Exception e) {
            log.error("创建/更新会话失败: ownerId={}, toId={}", ownerId, toId, e);
        }
    }

    private void setGroupMessageReadStatus(String messageId, String groupId, List<ImGroupMemberPo> members) {
        try {
            List<ImGroupMessageStatusPo> statusList = Optional.ofNullable(members)
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(m -> new ImGroupMessageStatusPo()
                            .setMessageId(messageId)
                            .setGroupId(groupId)
                            .setReadStatus(IMessageReadStatus.UNREAD.getCode())
                            .setToId(m.getMemberId()))
                    .collect(Collectors.toList());
            if (statusList.isEmpty()) {
                return;
            }
            groupMessageDubboService.creatBatch(statusList);
        } catch (Exception e) {
            log.error("设置群消息读状态失败: messageId={}", messageId, e);
        }
    }
}
