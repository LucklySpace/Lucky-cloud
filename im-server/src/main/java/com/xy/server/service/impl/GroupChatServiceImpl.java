package com.xy.server.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xy.imcore.enums.*;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.model.IMessageDto;
import com.xy.imcore.model.IMessageWrap;
import com.xy.server.config.RabbitTemplateFactory;
import com.xy.server.domain.dto.GroupDto;
import com.xy.server.domain.dto.GroupInviteDto;
import com.xy.server.domain.vo.GroupMemberVo;
import com.xy.server.exception.GlobalException;
import com.xy.server.mapper.ImChatMapper;
import com.xy.server.mapper.ImGroupMessageMapper;
import com.xy.server.mapper.ImUserDataMapper;
import com.xy.server.model.*;
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
        rabbitTemplate = rabbitTemplateFactory.createRabbitTemplate((correlationData, ack, cause) -> {
            if (ack) {
                log.info("group消息成功发送到交换机，消息ID: {}, ", correlationData != null ? correlationData.getId() : null);
            } else {
                log.error("group消息发送到交换机失败，原因: {}", cause);
                // 可以在此处做消息的重发或其他处理
            }
        }, (returnedMessage) -> {
            log.info("RabbitMQ-group交换机到队列退回:::return callback 消息体：{},应答码:{},原因:{},交换机:{},路由键:{}",
                    returnedMessage.getMessage(), returnedMessage.getReplyCode(), returnedMessage.getReplyText(),
                    returnedMessage.getExchange(), returnedMessage.getRoutingKey());
        });
    }

    /**
     * 发送群消息
     *
     * @param IMGroupMessageDto 群消息
     * @return
     */
    @Override
    @Transactional
    public Result send(IMGroupMessageDto IMGroupMessageDto) {

        // 消息id
        String message_id = IdUtil.getSnowflake().nextIdStr();

        // 群id
        String group_id = IMGroupMessageDto.getGroup_id();

        // 消息时间,使用utc时间
        long message_time = DateTimeUtils.getUTCDateTime();

        IMGroupMessageDto.setMessage_id(message_id);

        IMGroupMessageDto.setMessage_time(message_time);

        ImGroupMessage imGroupMessage = new ImGroupMessage();

        BeanUtils.copyProperties(IMGroupMessageDto, imGroupMessage);

        insertImGroupMessageAsync(imGroupMessage);

        // 数据库查询群成员
        QueryWrapper<ImGroupMember> groupMemberQuery = new QueryWrapper<>();

        groupMemberQuery.eq("group_id", group_id);

        List<ImGroupMember> imGroupMembers = imGroupMemberService.list(groupMemberQuery);

        // 使用并行流过滤和映射群成员
        List<String> to_List = imGroupMembers.parallelStream()
                .filter(groupMember -> !groupMember.getMember_id().equals(IMGroupMessageDto.getFrom_id()))
                .map(groupMember -> IMUSERPREFIX + groupMember.getMember_id())
                .collect(Collectors.toList());

        // 异步设置读取状态
        setReadStatusAsync(message_id, group_id, imGroupMembers);

        // 异步新增会话
        setChatAsync(group_id, message_time, imGroupMembers);

        // 根据群成员列表从redis获取用户 长连接信息
        List<Object> userObjList = redisUtil.batchGet(to_List);

        // brokerId进行分组map 键为brokerId 值为用户ID列表
        Map<String, List<String>> map = new HashMap<>();

        // 遍历群成员并处理用户信息，同时根据brokerId进行 netty分组，避免多次分发
        for (Object redisObj : userObjList) {

            if (ObjectUtil.isNotEmpty(redisObj)) {

                // 解析redis中的用户信息
                IMRegisterUserDto IMRegisterUserDto = JsonUtil.parseObject(redisObj, new TypeReference<IMRegisterUserDto>() {
                });

                // 获取brokerId
                String brokerId = IMRegisterUserDto.getBroker_id();

                // 获取用户ID
                String userId = IMRegisterUserDto.getUser_id();

                // 添加到map
                map.computeIfAbsent(brokerId, key -> new ArrayList<>()).add(userId);
            }
        }

        // 遍历map，将消息分发到各个长连接机器
        for (String brokerId : map.keySet()) {

            IMGroupMessageDto.setTo_List(map.get(brokerId));

            // 对发送消息进行包装
            IMessageWrap IMessageWrap = new IMessageWrap(IMessageType.GROUP_MESSAGE.getCode(), IMGroupMessageDto);

            // 创建 CorrelationData，并设置消息ID
            CorrelationData correlationData = new CorrelationData(message_id);

            // 发送到消息队列
            rabbitTemplate.convertAndSend(EXCHANGENAME, ROUTERKEYPREFIX + brokerId, JsonUtil.toJSONString(IMessageWrap),correlationData);
        }


        return Result.success(IMGroupMessageDto);
    }


    public void insertImGroupMessageAsync(ImGroupMessage imGroupMessage) {
        log.info("群号:{}  发送人:{}  消息内容:{}", imGroupMessage.getGroup_id(), imGroupMessage.getFrom_id(), imGroupMessage.getMessage_body());
        CompletableFuture.runAsync(() -> {
            imGroupMessageMapper.insert(imGroupMessage);
        });
    }

    private void setChatAsync(String group_id, long message_time, List<ImGroupMember> imGroupMembers) {
        CompletableFuture.runAsync(() -> {
            // 新增会话
            setChat(group_id, message_time, imGroupMembers);
        });
    }

    private void setReadStatusAsync(String messageId, String groupId, List<ImGroupMember> imGroupMembers) {
        CompletableFuture.runAsync(() -> {
            // 设置消息读取状态
            setReadStatus(messageId, groupId, imGroupMembers);
        });
    }


    /**
     * 设置消息读取状态
     *
     * @param messageId      消息主键
     * @param groupId        群号
     * @param imGroupMembers 群成员
     */
    @Transactional
    public void setReadStatus(String messageId, String groupId, List<ImGroupMember> imGroupMembers) {

        List<ImGroupMessageStatus> groupReadStatusList = new ArrayList<>();

        for (ImGroupMember imGroupMember : imGroupMembers) {
            ImGroupMessageStatus groupReadStatus = new ImGroupMessageStatus()
                    .setMessage_id(messageId)
                    .setGroup_id(groupId)
                    .setRead_status(IMessageReadStatus.UNREAD.code())
                    .setTo_id(imGroupMember.getMember_id());
            groupReadStatusList.add(groupReadStatus);
        }

        imGroupMessageStatusService.saveBatch(groupReadStatusList);
    }

    /**
     * 新增会话
     *
     * @param group_id       群号
     * @param message_time   消息时间
     * @param imGroupMembers 群成员
     */
    @Transactional
    public void setChat(String group_id, long message_time, List<ImGroupMember> imGroupMembers) {

        for (ImGroupMember imGroupMember : imGroupMembers) {

            // 查询会话是否存在
            QueryWrapper<ImChat> chatQuery = new QueryWrapper<>();
            chatQuery.eq("owner_id", imGroupMember.getMember_id());
            chatQuery.eq("to_id", group_id);
            chatQuery.eq("chat_type", IMessageType.GROUP_MESSAGE.getCode());

            ImChat imChat = imChatMapper.selectOne(chatQuery);

            if (ObjectUtil.isEmpty(imChat)) {
                imChat = new ImChat();
                String id = UUID.randomUUID().toString();

                imChat.setChat_id(id)
                        .setOwner_id(imGroupMember.getMember_id())
                        .setTo_id(group_id)
                        .setSequence(message_time)
                        .setIs_mute(IMStatus.NO.getCode())
                        .setIs_top(IMStatus.NO.getCode())
                        .setChat_type(IMessageType.GROUP_MESSAGE.getCode());

                imChatMapper.insert(imChat);
            } else {
                imChat.setSequence(message_time);
                imChatMapper.updateById(imChat);
            }

        }
    }

    @Override
    public Result member(GroupDto groupDto) {
        // 查询群成员列表
        List<ImGroupMember> imGroupMembers = imGroupMemberService.list(
                new QueryWrapper<ImGroupMember>().eq("group_id", groupDto.getGroup_id())
        );

        // 提取成员ID列表
        List<String> memberIdList = imGroupMembers.stream()
                .map(ImGroupMember::getMember_id)
                .collect(Collectors.toList());

        // 根据成员ID列表查询用户信息，并将其存储到 Map 中以便快速查找
        Map<String, ImUserData> userDataMap = imUserDataMapper.selectBatchIds(memberIdList)
                .stream()
                .collect(Collectors.toMap(ImUserData::getUser_id, Function.identity()));

        // 构建成员信息的 Map
        Map<String, GroupMemberVo> groupMemberVoMap = new HashMap<>();
        for (ImGroupMember imGroupMember : imGroupMembers) {
            ImUserData imUserData = userDataMap.get(imGroupMember.getMember_id());
            if (imUserData != null) {
                GroupMemberVo groupMemberVo = new GroupMemberVo();
                BeanUtils.copyProperties(imUserData, groupMemberVo);
                groupMemberVo.setRole(imGroupMember.getRole());
                groupMemberVo.setMute(imGroupMember.getMute());
                groupMemberVo.setAlias(imGroupMember.getAlias());
                //groupMemberVo.setJoin_type(imGroupMember.getJoin_type());
                groupMemberVoMap.put(imUserData.getUser_id(), groupMemberVo);
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
    public void quit(GroupDto groupDto) {

        String groupId = groupDto.getGroup_id();

        String userId = groupDto.getUser_id();

        // 查询群成员关系
        ImGroupMember imGroupMember = imGroupMemberService.getOne(new QueryWrapper<ImGroupMember>().eq("group_id", groupId).eq("member_id", userId));

        // 获取角色
        Integer role = imGroupMember.getRole();

        // 判断是否群主
        if (role.equals(IMemberStatus.GROUP_OWNER.getCode())) {
            throw new GlobalException(ResultEnum.ERROR, "群主不可退出群聊");
        }

        // 删除群成员关系
        imGroupMemberService.removeById(imGroupMember.getGroup_member_id());

        log.info("退出群聊，群聊id:{},用户id:{}", groupId, userId);
    }

    /**
     * 邀请成员
     *
     * @param groupInviteDto
     */
    @Override
    public Result invite(GroupInviteDto groupInviteDto) {
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

        String userId = groupInviteDto.getUser_id();

        List<String> friendIds = groupInviteDto.getMemberIds();

        long time = new Date().getTime();

        // 保存群成员关系
        List<ImGroupMember> imGroupMemberList = new ArrayList<>();

        // 邀请者默认为群主
        ImGroupMember imGroupMember = new ImGroupMember();

        imGroupMember.setGroup_id(groupId)
                .setGroup_member_id(IdUtil.getSnowflakeNextId())
                .setMember_id(userId)
                .setRole(IMemberStatus.GROUP_OWNER.getCode())
                .setMute(IMStatus.YES.getCode())
                .setJoin_time(time)
        ;

        imGroupMemberList.add(imGroupMember);

        // 被邀请者默认为普通成员
        for (String friendId : friendIds) {
            ImGroupMember groupMember = new ImGroupMember();
            groupMember.setGroup_id(groupId)
                    .setGroup_member_id(IdUtil.getSnowflakeNextId())
                    .setMember_id(friendId)
                    .setRole(IMemberStatus.NORMAL.getCode())
                    .setMute(IMStatus.YES.getCode())
                    .setJoin_time(time)
            ;

            imGroupMemberList.add(groupMember);
        }

        imGroupMemberService.saveBatch(imGroupMemberList);


        // 保存群聊
        ImGroup imGroup = new ImGroup();
        imGroup.setGroup_id(groupId)
                .setOwner_id(userId)
                .setGroup_name("默认群聊-" + code)
                .setAvatar(generateGroupAvatar(groupId))
                .setStatus(IMStatus.YES.getCode())
                .setCreate_time(time)
        ;

        imGroupService.save(imGroup);

        // 发送系统群聊邀请消息,系统消息默认用户000000
        IMGroupMessageDto IMGroupMessageDto = (IMGroupMessageDto)systemMessage(groupId,"加入群聊,请尽情聊天吧");

        // 发送群聊消息
        send(IMGroupMessageDto);

        log.info("新建群聊，群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());

        return Result.success(groupId);
    }


    /**
     * 系统消息
     * @param groupId 群聊id
     * @param message 消息内容
     * @return
     */
    public IMGroupMessageDto systemMessage(String groupId, String message) {
        IMGroupMessageDto imGroupMessageDto = new IMGroupMessageDto();
        imGroupMessageDto.setGroup_id(groupId)
                .setFrom_id("000000") // 系统消息默认用户000000
                .setMessage_content_type(String.valueOf(IMessageContentType.TIP.getCode())) // 系统消息
                .setMessage_body(new IMessageDto.TextMessageBody().setMessage(message)); // 群聊邀请消息
        return imGroupMessageDto;
    }


    public Result groupInvite(GroupInviteDto groupInviteDto) {

        String groupId = groupInviteDto.getGroup_id();

        if(!StringUtils.hasText(groupInviteDto.getGroup_id())) {
            groupId = UUID.randomUUID().toString();
        }

        String userId = groupInviteDto.getUser_id();
        List<String> friendIds = groupInviteDto.getMemberIds();

        // 查询群成员列表并转换为 Set
        Set<String> memberIdSet = imGroupMemberService.list(new QueryWrapper<ImGroupMember>().eq("group_id", groupId))
                .stream()
                .map(ImGroupMember::getMember_id)
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
            List<ImGroupMember> newGroupMemberList = createNewGroupMembers(groupId, newMemberList);
            imGroupMemberService.saveBatch(newGroupMemberList);
        }

        // 更新群头像
        ImGroup imGroup = new ImGroup();
        imGroup.setGroup_id(groupId)
                .setAvatar(generateGroupAvatar(groupId));
        imGroupService.updateById(imGroup);

        log.info("邀请成员，群聊id:{},用户id:{},新成员id:{}", groupId, userId, newMemberList.toString());
        return Result.success(groupId);
    }


    private List<ImGroupMember> createNewGroupMembers(String groupId, List<String> newMemberList) {
        long joinTime = new Date().getTime();
        return newMemberList.stream()
                .map(memberId -> {
                    ImGroupMember imGroupMember = new ImGroupMember();
                    imGroupMember.setGroup_id(groupId); // 群聊id
                    imGroupMember.setMember_id(memberId);  // 成员id
                    imGroupMember.setRole(IMemberStatus.NORMAL.getCode()); // 成员角色
                    imGroupMember.setMute(IMStatus.YES.getCode()); // 是否禁言
                    imGroupMember.setJoin_time(joinTime); // 加入时间
                    return imGroupMember;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Result info(GroupDto groupDto) {
        String groupId = groupDto.getGroup_id();
        ImGroup imGroup = imGroupService.getOne(new QueryWrapper<ImGroup>().eq("group_id", groupId));
        return Result.success(imGroup);
    }

    /**
     * 生成群聊头像
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
