package com.xy.server.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.imcore.enums.*;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.model.IMessageDto;
import com.xy.imcore.model.IMessageWrap;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.domain.dto.GroupDto;
import com.xy.server.domain.dto.GroupInviteDto;
import com.xy.server.domain.po.*;
import com.xy.server.domain.vo.GroupMemberVo;
import com.xy.server.exception.GlobalException;
import com.xy.server.mapper.ImChatMapper;
import com.xy.server.mapper.ImGroupMessageMapper;
import com.xy.server.mapper.ImUserDataMapper;
import com.xy.server.response.Result;
import com.xy.server.response.ResultEnum;
import com.xy.server.service.*;
import com.xy.server.utils.DateTimeUtils;
import com.xy.server.utils.GroupHeadImageUtil;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.xy.imcore.constants.Constant.*;


@Slf4j
@Service
public class GroupChatServiceImpl implements GroupChatService {

    // 头像工具类，用于生成九宫格
    GroupHeadImageUtil groupHeadImageUtil = new GroupHeadImageUtil();
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ImGroupMessageMapper imGroupMessageMapper;
    @Resource
    private ImGroupService imGroupService;
    @Resource
    private ImGroupMemberService imGroupMemberService;
    @Resource
    private ImGroupMessageStatusService imGroupMessageStatusService;
    @Resource
    private ImChatMapper imChatMapper;
    @Resource
    private ImUserDataMapper imUserDataMapper;
    @Resource
    private FileService fileService;
    @Resource
    private RabbitTemplateFactory rabbitTemplateFactory;
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init() {
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate(
                (correlationData, ack, cause) -> {
                    if (ack) {
                        log.info("group消息成功发送到交换机，消息ID: {}",
                                correlationData != null ? correlationData.getId() : null);
                    } else {
                        log.error("group消息发送到交换机失败，原因: {}", cause);
                    }
                },
                returnedMessage -> log.warn("RabbitMQ-group消息退回: 消息体:{}, 应答码:{}, 原因:{}, 交换机:{}, 路由键:{}",
                        returnedMessage.getMessage(), returnedMessage.getReplyCode(),
                        returnedMessage.getReplyText(), returnedMessage.getExchange(), returnedMessage.getRoutingKey())
        );
    }

    /**
     * 发送群聊消息
     *
     * @param imGroupMessageDto 群消息对象
     * @return 发送结果
     */
    @Override
    @Transactional
    public Result send(IMGroupMessageDto imGroupMessageDto) {
        try {

            String messageId = IdUtil.getSnowflake().nextIdStr();

            String groupId = imGroupMessageDto.getGroupId();

            Long messageTime = DateTimeUtils.getUTCDateTime();

            imGroupMessageDto.setMessageId(messageId)
                    .setMessageTime(messageTime);

            // 异步插入群消息
            insertImGroupMessageAsync(imGroupMessageDto);

            // 获取群成员列表
            List<ImGroupMemberPo> imGroupMemberPos = imGroupMemberService.list(new QueryWrapper<ImGroupMemberPo>()
                    .eq("group_id", groupId));

            if (CollectionUtil.isEmpty(imGroupMemberPos)) {
                log.warn("群:{} 没有成员，无法发送消息", groupId);
                return Result.failed("群聊成员为空，无法发送消息");
            }

            // 过滤掉发送者，获取接收者ID列表
            List<String> toList = imGroupMemberPos.parallelStream()
                    .filter(member -> !member.getMemberId().equals(imGroupMessageDto.getFromId()))
                    .map(member -> IMUSERPREFIX + member.getMemberId())
                    .collect(Collectors.toList());

            // 异步设置消息读取状态
            setReadStatusAsync(messageId, groupId, imGroupMemberPos);

            // 异步更新会话信息
            setChatAsync(groupId, messageTime, imGroupMemberPos);

            // 批量获取用户的长连接信息
            List<Object> userObjList = redisUtil.batchGet(toList);
            Map<String, List<String>> brokerMap = new HashMap<>();

            // 根据长连接的  brokerId 对用户进行分类汇总
            for (Object redisObj : userObjList) {
                if (ObjectUtil.isNotEmpty(redisObj)) {
                    IMRegisterUserDto userDto = JsonUtil.parseObject(redisObj, IMRegisterUserDto.class);
                    brokerMap.computeIfAbsent(userDto.getBrokerId(), k -> new ArrayList<>()).add(userDto.getUserId());
                }
            }

            // 分发消息到不同的 broker
            for (Map.Entry<String, List<String>> entry : brokerMap.entrySet()) {
                String brokerId = entry.getKey();
                imGroupMessageDto.setTo_List(entry.getValue());

                IMessageWrap messageWrap = new IMessageWrap(IMessageType.GROUP_MESSAGE.getCode(), imGroupMessageDto);
                CorrelationData correlationData = new CorrelationData(messageId);

                rabbitTemplate.convertAndSend(EXCHANGENAME, ROUTERKEYPREFIX + brokerId,
                        Objects.requireNonNull(JsonUtil.toJSONString(messageWrap)), correlationData);
            }

            return Result.success(imGroupMessageDto);

        } catch (Exception e) {
            log.error("群消息发送失败, 群ID: {}, 发送者: {}, 错误: {}",
                    imGroupMessageDto.getGroupId(), imGroupMessageDto.getFromId(), e.getMessage(), e);
            return Result.failed("发送群消息失败");
        }
    }

    /**
     * 异步插入群消息
     */
    private void insertImGroupMessageAsync(IMGroupMessageDto imGroupMessageDto) {
        CompletableFuture.runAsync(() -> {
            try {
                ImGroupMessagePo imGroupMessagePo = new ImGroupMessagePo();
                BeanUtils.copyProperties(imGroupMessageDto, imGroupMessagePo);
                imGroupMessageMapper.insert(imGroupMessagePo);
                log.info("群消息插入成功, 群ID: {}, 发送者: {}", imGroupMessagePo.getGroupId(), imGroupMessagePo.getFromId());
            } catch (Exception e) {
                log.error("异步插入群消息失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 异步更新会话信息
     */
    private void setChatAsync(String groupId, Long messageTime, List<ImGroupMemberPo> imGroupMemberPos) {
        CompletableFuture.runAsync(() -> setChat(groupId, messageTime, imGroupMemberPos));
    }

    /**
     * 异步设置消息阅读状态
     */
    private void setReadStatusAsync(String messageId, String groupId, List<ImGroupMemberPo> imGroupMemberPos) {
        CompletableFuture.runAsync(() -> setReadStatus(messageId, groupId, imGroupMemberPos));
    }

    /**
     * 设置群消息的阅读状态
     */
    @Transactional
    public void setReadStatus(String messageId, String groupId, List<ImGroupMemberPo> imGroupMemberPos) {
        try {
            List<ImGroupMessageStatusPo> groupReadStatusList = imGroupMemberPos.stream()
                    .map(member -> new ImGroupMessageStatusPo()
                            .setMessageId(messageId)
                            .setGroupId(groupId)
                            .setReadStatus(IMessageReadStatus.UNREAD.code())
                            .setToId(member.getMemberId()))
                    .collect(Collectors.toList());

            imGroupMessageStatusService.saveBatch(groupReadStatusList);
            log.info("成功设置群消息阅读状态, 消息ID: {}", messageId);
        } catch (Exception e) {
            log.error("设置群消息阅读状态失败, 消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * 更新群聊会话信息
     */
    @Transactional
    public void setChat(String groupId, Long messageTime, List<ImGroupMemberPo> imGroupMemberPos) {
        try {
            for (ImGroupMemberPo member : imGroupMemberPos) {
                ImChatPo chat = imChatMapper.selectOne(new QueryWrapper<ImChatPo>()
                        .eq("owner_id", member.getMemberId())
                        .eq("to_id", groupId)
                        .eq("chat_type", IMessageType.GROUP_MESSAGE.getCode()));

                if (ObjectUtil.isEmpty(chat)) {
                    chat = new ImChatPo();
                    chat.setChatId(UUID.randomUUID().toString())
                            .setOwnerId(member.getMemberId())
                            .setToId(groupId)
                            .setSequence(messageTime)
                            .setIsMute(IMStatus.NO.getCode())
                            .setIsTop(IMStatus.NO.getCode())
                            .setChatType(IMessageType.GROUP_MESSAGE.getCode());
                    imChatMapper.insert(chat);
                } else {
                    chat.setSequence(messageTime);
                    imChatMapper.updateById(chat);
                }
            }
            log.info("成功更新群会话信息, 群ID: {}", groupId);
        } catch (Exception e) {
            log.error("更新群会话信息失败, 群ID: {}, 错误: {}", groupId, e.getMessage(), e);
        }
    }


    @Override
    public Result getMembers(GroupDto groupDto) {
        // 查询群成员列表
        List<ImGroupMemberPo> imGroupMemberPos = imGroupMemberService.list(
                new QueryWrapper<ImGroupMemberPo>().eq("group_id", groupDto.getGroupId())
        );

        // 提取成员ID列表
        List<String> memberIdList = imGroupMemberPos.stream()
                .map(ImGroupMemberPo::getMemberId)
                .collect(Collectors.toList());

        // 根据成员ID列表查询用户信息，并将其存储到 Map 中以便快速查找
        Map<String, ImUserDataPo> userDataMap = imUserDataMapper.selectBatchIds(memberIdList)
                .stream()
                .collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));

        // 构建成员信息的 Map
        Map<String, GroupMemberVo> groupMemberVoMap = new HashMap<>();
        for (ImGroupMemberPo imGroupMemberPo : imGroupMemberPos) {
            ImUserDataPo imUserDataPo = userDataMap.get(imGroupMemberPo.getMemberId());
            if (imUserDataPo != null) {
                GroupMemberVo groupMemberVo = new GroupMemberVo();
                BeanUtils.copyProperties(imUserDataPo, groupMemberVo);
                groupMemberVo.setRole(imGroupMemberPo.getRole())
                        .setMute(imGroupMemberPo.getMute())
                        .setAlias(imGroupMemberPo.getAlias());
                //groupMemberVo.setJoin_type(imGroupMember.getJoin_type());
                groupMemberVoMap.put(imUserDataPo.getUserId(), groupMemberVo);
            }
        }

        return Result.success(groupMemberVoMap);
    }

    /**
     * 退出群聊
     *
     * @param groupDto
     */
    @Override
    public void quitGroup(GroupDto groupDto) {

        String groupId = groupDto.getGroupId();

        String userId = groupDto.getUserId();

        // 查询群成员关系
        ImGroupMemberPo imGroupMemberPo = imGroupMemberService.getOne(new QueryWrapper<ImGroupMemberPo>().eq("group_id", groupId).eq("member_id", userId));

        // 获取角色
        Integer role = imGroupMemberPo.getRole();

        // 判断是否群主
        if (role.equals(IMemberStatus.GROUP_OWNER.getCode())) {
            throw new GlobalException(ResultEnum.ERROR, "群主不可退出群聊");
        }

        // 删除群成员关系
        imGroupMemberService.removeById(imGroupMemberPo.getGroupMemberId());

        log.info("退出群聊，群聊id:{},用户id:{}", groupId, userId);
    }

    /**
     * 邀请成员
     *
     * @param groupInviteDto
     */
    @Override
    @Transactional
    public Result inviteGroup(GroupInviteDto groupInviteDto) {
        Integer type = groupInviteDto.getType();

        if (type.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
            return singleInvite(groupInviteDto);
        }

        if (type.equals(IMessageType.GROUP_MESSAGE.getCode())) {
            return groupInvite(groupInviteDto);
        }
        return null;
    }

    public Result singleInvite(GroupInviteDto groupInviteDto) {

        String groupId = UUID.randomUUID().toString();

        String code = String.valueOf((int) ((Math.random() * 9 + 1) * Math.pow(10, 5)));

        String userId = groupInviteDto.getUserId();

        List<String> friendIds = groupInviteDto.getMemberIds();

        long joinTime = new Date().getTime();

        // 保存群成员关系
        List<ImGroupMemberPo> imGroupMemberPoList = new ArrayList<>();

        // 邀请者默认为群主
        ImGroupMemberPo imGroupMemberPo = new ImGroupMemberPo();

        imGroupMemberPo.setGroupId(groupId)
                .setGroupMemberId(IdUtil.getSnowflakeNextId())
                .setMemberId(userId)
                .setRole(IMemberStatus.GROUP_OWNER.getCode())
                .setMute(IMStatus.YES.getCode())
                .setJoinTime(joinTime);

        imGroupMemberPoList.add(imGroupMemberPo);

        // 被邀请者默认为普通成员
        for (String friendId : friendIds) {
            ImGroupMemberPo groupMember = new ImGroupMemberPo();
            groupMember.setGroupId(groupId)
                    .setGroupMemberId(IdUtil.getSnowflakeNextId())
                    .setMemberId(friendId)
                    .setRole(IMemberStatus.NORMAL.getCode())
                    .setMute(IMStatus.YES.getCode())
                    .setJoinTime(joinTime);

            imGroupMemberPoList.add(groupMember);
        }

        // 批量插入成员信息
        imGroupMemberService.saveBatch(imGroupMemberPoList);

        // 保存群聊
        ImGroupPo imGroupPo = new ImGroupPo();
        imGroupPo.setGroupId(groupId)
                .setOwnerId(userId)
                .setGroupName("默认群聊-" + code)
                .setAvatar(generateGroupAvatar(groupId))
                .setStatus(IMStatus.YES.getCode())
                .setCreateTime(joinTime);

        imGroupService.save(imGroupPo);

        // 发送系统群聊邀请消息,系统消息默认用户000000
        IMGroupMessageDto IMGroupMessageDto = (IMGroupMessageDto) systemMessage(groupId, "加入群聊,请尽情聊天吧");

        // 发送群聊消息
        send(IMGroupMessageDto);

        log.info("新建群聊，群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());

        return Result.success(groupId);
    }


    /**
     * 系统消息
     *
     * @param groupId 群聊id
     * @param message 消息内容
     * @return
     */
    public IMGroupMessageDto systemMessage(String groupId, String message) {
        IMGroupMessageDto imGroupMessageDto = new IMGroupMessageDto();
        imGroupMessageDto.setGroupId(groupId)
                .setFromId("000000") // 系统消息默认用户000000
                .setMessageContentType(IMessageContentType.TIP.getCode()) // 系统消息
                .setMessageBody(new IMessageDto.TextMessageBody().setMessage(message)); // 群聊邀请消息
        return imGroupMessageDto;
    }


    public Result groupInvite(GroupInviteDto groupInviteDto) {

        String groupId = groupInviteDto.getGroupId();

        if (!StringUtils.hasText(groupInviteDto.getGroupId())) {
            groupId = UUID.randomUUID().toString();
        }

        String userId = groupInviteDto.getUserId();
        List<String> friendIds = groupInviteDto.getMemberIds();

        // 查询群成员列表并转换为 Set
        Set<String> memberIdSet = imGroupMemberService.list(new QueryWrapper<ImGroupMemberPo>().eq("group_id", groupId))
                .stream()
                .map(ImGroupMemberPo::getMemberId)
                .collect(Collectors.toSet());

        if (!memberIdSet.contains(userId)) {
            throw new GlobalException(ResultEnum.ERROR, "用户不在该群组中，不可邀请");
        }

        // 过滤出不在群组中的新成员
        List<String> newMemberList = friendIds.stream()
                .filter(friendId -> !memberIdSet.contains(friendId))
                .collect(Collectors.toList());

        // 若有新成员，则添加到群组中
        if (!newMemberList.isEmpty()) {
            List<ImGroupMemberPo> newGroupMemberList = createNewGroupMembers(groupId, newMemberList);
            imGroupMemberService.saveBatch(newGroupMemberList);
        }

        // 更新群头像
        ImGroupPo imGroupPo = new ImGroupPo();
        imGroupPo.setGroupId(groupId)
                .setAvatar(generateGroupAvatar(groupId));
        imGroupService.updateById(imGroupPo);

        log.info("邀请成员，群聊id:{},用户id:{},新成员id:{}", groupId, userId, newMemberList.toString());
        return Result.success(groupId);
    }


    private List<ImGroupMemberPo> createNewGroupMembers(String groupId, List<String> newMemberList) {
        long joinTime = new Date().getTime();
        return newMemberList.stream()
                .map(memberId -> {
                    ImGroupMemberPo imGroupMemberPo = new ImGroupMemberPo();
                    imGroupMemberPo.setGroupId(groupId); // 群聊id
                    imGroupMemberPo.setMemberId(memberId);  // 成员id
                    imGroupMemberPo.setRole(IMemberStatus.NORMAL.getCode()); // 成员角色
                    imGroupMemberPo.setMute(IMStatus.YES.getCode()); // 是否禁言
                    imGroupMemberPo.setJoinTime(joinTime); // 加入时间
                    return imGroupMemberPo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result groupInfo(GroupDto groupDto) {
        String groupId = groupDto.getGroupId();
        ImGroupPo imGroupPo = imGroupService.getOne(new QueryWrapper<ImGroupPo>().eq("group_id", groupId));
        return Result.success(imGroupPo);
    }

    /**
     * 生成群聊头像
     *
     * @param groupId 群聊id
     * @return 头像url
     */
    public String generateGroupAvatar(String groupId) {

        // 随机查询 9 个群成员头像
        List<String> avatarList = imGroupService.selectNinePeople(groupId);

        // 生成群聊头像
        File groupHead = groupHeadImageUtil.getCombinationOfhead(avatarList, "defaultGroupHead" + groupId);

        MultipartFile multipartFile = fileService.fileToImageMultipartFile(groupHead);

        return fileService.uploadFile(multipartFile);
    }

}
