package com.xy.lucky.chat.service.impl;

import com.xy.lucky.chat.common.LockExecutor;
import com.xy.lucky.chat.domain.dto.ChatDto;
import com.xy.lucky.chat.domain.mapper.ChatBeanMapper;
import com.xy.lucky.chat.domain.vo.ChatVo;
import com.xy.lucky.chat.exception.ChatException;
import com.xy.lucky.chat.service.ChatService;
import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.domain.po.*;
import com.xy.lucky.rpc.api.database.chat.ImChatDubboService;
import com.xy.lucky.rpc.api.database.group.ImGroupDubboService;
import com.xy.lucky.rpc.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.rpc.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.rpc.api.database.user.ImUserDataDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 会话服务实现
 *
 * @author xy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /**
     * 分布式锁前缀
     */
    private static final String LOCK_PREFIX = "lock:chat:";
    private final LockExecutor lockExecutor;
    @DubboReference
    private ImChatDubboService chatDubboService;
    @DubboReference
    private ImUserDataDubboService userDataDubboService;
    @DubboReference
    private ImGroupDubboService groupDubboService;
    @DubboReference
    private ImSingleMessageDubboService singleMessageDubboService;
    @DubboReference
    private ImGroupMessageDubboService groupMessageDubboService;

    /**
     * 标记消息已读
     *
     * @param dto 会话信息（已在 Controller 层校验）
     */
    @Override
    public void read(ChatDto dto) {
        String lockKey = LOCK_PREFIX + "read:" + dto.getChatType() + ":" + dto.getFromId() + ":" + dto.getToId();
        lockExecutor.execute(lockKey, () -> {
            IMessageType type = IMessageType.getByCode(dto.getChatType());
            if (type == null) {
                throw new ChatException("不支持的消息类型");
            }

            switch (type) {
                case SINGLE_MESSAGE -> markSingleMessageRead(dto);
                case GROUP_MESSAGE -> markGroupMessageRead(dto);
                default -> throw new ChatException("不支持的消息类型");
            }
        });
    }

    /**
     * 创建会话
     *
     * @param dto 会话信息（已在 Controller 层校验）
     * @return 会话详情
     */
    @Override
    public ChatVo create(ChatDto dto) {
        String lockKey = LOCK_PREFIX + "create:" + dto.getFromId() + ":" + dto.getToId() + ":" + dto.getChatType();
        return lockExecutor.execute(lockKey, () -> {
            // 查询是否已存在
            ImChatPo existingChat = chatDubboService.queryOne(dto.getFromId(), dto.getToId(), dto.getChatType());
            ImChatPo chatPo = existingChat != null ? existingChat : createNewChat(dto);
            return enrichChatVo(chatPo, dto.getChatType());
        });
    }

    /**
     * 查询单个会话
     *
     * @param ownerId 所有者ID（已在 Controller 层校验）
     * @param toId    目标ID（已在 Controller 层校验）
     * @return 会话详情
     */
    @Override
    public ChatVo one(String ownerId, String toId) {
        String lockKey = LOCK_PREFIX + "one:" + ownerId + ":" + toId;
        return lockExecutor.execute(lockKey, () -> {
            ImChatPo chatPo = chatDubboService.queryOne(ownerId, toId, null);
            return chatPo != null ? buildChatVo(chatPo) : new ChatVo();
        });
    }

    /**
     * 查询会话列表
     *
     * @param dto 查询条件（已在 Controller 层校验）
     * @return 会话列表
     */
    @Override
    public List<ChatVo> list(ChatDto dto) {
        String lockKey = LOCK_PREFIX + "list:" + dto.getFromId();
        return lockExecutor.execute(lockKey, () -> {
            List<ImChatPo> chatList = chatDubboService.queryList(dto.getFromId(), dto.getSequence());

            if (CollectionUtils.isEmpty(chatList)) {
                return Collections.emptyList();
            }

            return chatList.stream()
                    .map(this::buildChatVo)
                    .toList();
        });
    }

    // ==================== 私有方法 ====================

    /**
     * 创建新会话
     */
    private ImChatPo createNewChat(ChatDto dto) {
        ImChatPo chatPo = new ImChatPo()
                .setChatId(UUID.randomUUID().toString())
                .setOwnerId(dto.getFromId())
                .setToId(dto.getToId())
                .setChatType(dto.getChatType())
                .setIsMute(IMStatus.NO.getCode())
                .setIsTop(IMStatus.NO.getCode());

        if (!chatDubboService.creat(chatPo)) {
            throw new ChatException("创建会话失败");
        }

        log.info("创建会话成功: chatId={}, from={}, to={}", chatPo.getChatId(), dto.getFromId(), dto.getToId());
        return chatPo;
    }

    /**
     * 构建会话 VO（根据类型选择策略）
     */
    private ChatVo buildChatVo(ImChatPo chatPo) {
        if (chatPo == null) {
            return new ChatVo();
        }

        IMessageType type = IMessageType.getByCode(chatPo.getChatType());
        if (type == null) {
            return new ChatVo();
        }

        return switch (type) {
            case SINGLE_MESSAGE -> buildSingleChatVo(chatPo);
            case GROUP_MESSAGE -> buildGroupChatVo(chatPo);
            default -> new ChatVo();
        };
    }

    /**
     * 构建单聊会话 VO
     */
    private ChatVo buildSingleChatVo(ImChatPo chatPo) {
        ChatVo vo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        String ownerId = vo.getOwnerId();
        String toId = vo.getToId();

        // 获取最后一条消息
        ImSingleMessagePo lastMsg = singleMessageDubboService.queryLast(ownerId, toId);
        if (lastMsg != null && lastMsg.getMessageId() != null) {
            vo.setMessage(lastMsg.getMessageBody());
            vo.setMessageContentType(lastMsg.getMessageContentType());
            vo.setMessageTime(lastMsg.getMessageTime());
        } else {
            vo.setMessageTime(0L);
        }

        // 获取未读数
        Integer unread = singleMessageDubboService.queryReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.getCode());
        vo.setUnread(unread != null ? unread : 0);

        // 获取用户信息
        String targetUserId = ownerId.equals(toId) ? ownerId : toId;
        ImUserDataPo user = userDataDubboService.queryOne(targetUserId);
        if (user != null) {
            vo.setId(user.getUserId());
            vo.setName(user.getName());
            vo.setAvatar(user.getAvatar());
        }

        return vo;
    }

    /**
     * 构建群聊会话 VO
     */
    private ChatVo buildGroupChatVo(ImChatPo chatPo) {
        ChatVo vo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);
        String ownerId = vo.getOwnerId();
        String groupId = vo.getToId();

        // 获取最后一条消息
        ImGroupMessagePo lastMsg = groupMessageDubboService.queryLast(ownerId, groupId);
        if (lastMsg != null && lastMsg.getMessageId() != null) {
            vo.setMessage(lastMsg.getMessageBody());
            vo.setMessageTime(lastMsg.getMessageTime());
        } else {
            vo.setMessageTime(0L);
        }

        // 获取未读数
        Integer unread = groupMessageDubboService.queryReadStatus(groupId, ownerId, IMessageReadStatus.UNREAD.getCode());
        vo.setUnread(unread != null ? unread : 0);

        // 获取群信息
        ImGroupPo group = groupDubboService.queryOne(groupId);
        if (group != null) {
            vo.setId(group.getGroupId());
            vo.setName(group.getGroupName());
            vo.setAvatar(group.getAvatar());
        }

        return vo;
    }

    /**
     * 丰富会话 VO（用于创建会话后返回）
     */
    private ChatVo enrichChatVo(ImChatPo chatPo, Integer chatType) {
        ChatVo vo = ChatBeanMapper.INSTANCE.toChatVo(chatPo);

        if (IMessageType.SINGLE_MESSAGE.getCode().equals(chatType)) {
            ImUserDataPo user = userDataDubboService.queryOne(vo.getToId());
            if (user != null) {
                vo.setId(user.getUserId());
                vo.setName(user.getName());
                vo.setAvatar(user.getAvatar());
            }
        } else if (IMessageType.GROUP_MESSAGE.getCode().equals(chatType)) {
            ImGroupPo group = groupDubboService.queryOne(vo.getToId());
            if (group != null) {
                vo.setId(group.getGroupId());
                vo.setName(group.getGroupName());
                vo.setAvatar(group.getAvatar());
            }
        }

        return vo;
    }

    /**
     * 标记单聊消息已读
     */
    private void markSingleMessageRead(ChatDto dto) {
        ImSingleMessagePo updatePo = new ImSingleMessagePo()
                .setReadStatus(IMessageReadStatus.ALREADY_READ.getCode())
                .setFromId(dto.getFromId())
                .setToId(dto.getToId());
        singleMessageDubboService.modifyReadStatus(updatePo);
    }

    /**
     * 标记群聊消息已读
     */
    private void markGroupMessageRead(ChatDto dto) {
        ImGroupMessageStatusPo updatePo = new ImGroupMessageStatusPo()
                .setReadStatus(IMessageReadStatus.ALREADY_READ.getCode())
                .setGroupId(dto.getFromId())
                .setToId(dto.getToId());
        groupMessageDubboService.modifyReadStatus(updatePo);
    }
}
