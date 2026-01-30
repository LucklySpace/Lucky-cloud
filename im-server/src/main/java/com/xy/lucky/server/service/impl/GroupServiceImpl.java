package com.xy.lucky.server.service.impl;

import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.*;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMessage;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupInviteRequestDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.web.api.id.ImIdDubboService;
import com.xy.lucky.server.common.LockExecutor;
import com.xy.lucky.server.config.IdGeneratorConstant;
import com.xy.lucky.server.domain.dto.GroupDto;
import com.xy.lucky.server.domain.dto.GroupInviteDto;
import com.xy.lucky.server.domain.dto.GroupMemberDto;
import com.xy.lucky.server.domain.mapper.GroupMemberBeanMapper;
import com.xy.lucky.server.domain.vo.FileVo;
import com.xy.lucky.server.domain.vo.GroupMemberVo;
import com.xy.lucky.server.exception.GroupException;
import com.xy.lucky.server.service.FileService;
import com.xy.lucky.server.service.GroupService;
import com.xy.lucky.server.service.MessageService;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.image.GroupHeadImageUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 群组服务实现
 *
 * @author xy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    /**
     * 缓存相关常量
     */
    private static final String CACHE_GROUP_INFO = "group:info:";
    private static final long CACHE_TTL_SECONDS = 300L;

    /** 分布式锁前缀 */
    private static final String LOCK_PREFIX = "lock:group:";

    /** 群头像生成工具 */
    private final GroupHeadImageUtils groupHeadImageUtils = new GroupHeadImageUtils();

    @DubboReference
    private ImUserDataDubboService userDataDubboService;
    @DubboReference
    private ImGroupDubboService groupDubboService;
    @DubboReference
    private ImGroupMemberDubboService groupMemberDubboService;
    @DubboReference
    private ImGroupInviteRequestDubboService groupInviteRequestDubboService;
    @DubboReference
    private ImIdDubboService idDubboService;
    private final RedissonClient redissonClient;
    private final LockExecutor lockExecutor;
    @Resource
    private MessageService messageService;
    @Resource
    private FileService fileService;

    /**
     * 获取群成员列表
     *
     * @param dto 群组信息（已在 Controller 层校验）
     * @return 成员映射表 (userId -> memberVo)
     */
    @Override
    public Map<?, ?> getGroupMembers(GroupDto dto) {
        List<ImGroupMemberPo> members = groupMemberDubboService.queryList(dto.getGroupId());
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyMap();
        }

        // 批量获取用户信息
        List<String> memberIds = members.stream().map(ImGroupMemberPo::getMemberId).toList();
        Map<String, ImUserDataPo> userMap = getUserMap(memberIds);

        // 构建返回结果
        Map<String, GroupMemberVo> result = new HashMap<>(members.size());
        for (ImGroupMemberPo member : members) {
            ImUserDataPo user = userMap.get(member.getMemberId());
            if (user != null) {
                GroupMemberVo vo = buildGroupMemberVo(member, user);
                result.put(user.getUserId(), vo);
            }
        }
        return result;
    }

    /**
     * 退出群聊
     *
     * @param dto 群组信息（已在 Controller 层校验）
     */
    @Override
    public void quitGroup(GroupDto dto) {
        String lockKey = LOCK_PREFIX + "quit:" + dto.getGroupId() + ":" + dto.getUserId();
        lockExecutor.execute(lockKey, () -> {
            ImGroupMemberPo member = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
            if (member == null) {
                throw new GroupException("用户不在群聊中");
            }
            if (IMemberStatus.GROUP_OWNER.getCode().equals(member.getRole())) {
                throw new GroupException("群主不可退出群聊");
            }

            boolean success = groupMemberDubboService.removeOne(member.getGroupMemberId());
            if (success) {
                log.info("退出群聊成功: groupId={}, userId={}", dto.getGroupId(), dto.getUserId());
            }
        });
    }

    /**
     * 邀请/创建群组
     *
     * @param dto 邀请信息（已在 Controller 层校验）
     * @return 群组 ID
     */
    @Override
    public String inviteGroup(GroupInviteDto dto) {
        // 创建群聊
        if (IMessageType.CREATE_GROUP.getCode().equals(dto.getType())) {
            return createGroup(dto);
        }

        // 邀请入群
        if (IMessageContentType.GROUP_INVITE.getCode().equals(dto.getType())) {
            return inviteToGroup(dto);
        }

        throw new GroupException("无效邀请类型");
    }

    /**
     * 审批群邀请
     *
     * @param dto 审批信息（已在 Controller 层校验）
     * @return 处理结果
     */
    @Override
    public String approveGroupInvite(GroupInviteDto dto) {
        // 待处理/已拒绝状态直接返回
        if (IMApproveStatus.PENDING.getCode().equals(dto.getApproveStatus())) {
            return "待处理群聊邀请";
        }
        if (IMApproveStatus.REJECTED.getCode().equals(dto.getApproveStatus())) {
            return "已拒绝群聊邀请";
        }

        // 检查群是否存在及是否允许加入
        ImGroupPo group = groupDubboService.queryOne(dto.getGroupId());
        if (group == null) {
            throw new GroupException("群不存在");
        }

        if (ImGroupJoinStatus.BAN.getCode().equals(group.getApplyJoinType())) {
            return "群不允许加入";
        }

        // 需要审批
        if (ImGroupJoinStatus.APPROVE.getCode().equals(group.getApplyJoinType())) {
            sendJoinApprovalRequest(dto.getGroupId(), dto.getInviterId(), dto.getUserId(), group);
            return "已发送入群验证请求，等待审核";
        }

        // 直接加入
        return processDirectJoin(dto.getGroupId(), dto.getUserId(), dto.getInviterId());
    }

    /**
     * 获取群信息
     *
     * @param dto 查询条件（已在 Controller 层校验）
     * @return 群信息
     */
    @Override
    public ImGroupPo groupInfo(GroupDto dto) {
        // 先查缓存
        RMapCache<String, Object> cache = redissonClient.getMapCache(CACHE_GROUP_INFO);
        Object cached = cache.get(dto.getGroupId());
        if (cached instanceof ImGroupPo cachedGroup) {
            return cachedGroup;
        }

        // 查数据库
        ImGroupPo group = groupDubboService.queryOne(dto.getGroupId());
        if (group != null) {
            cache.fastPut(dto.getGroupId(), group, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            return group;
        }

        return new ImGroupPo();
    }

    /**
     * 更新群信息
     *
     * @param dto 群信息（已在 Controller 层校验）
     * @return 是否成功
     */
    @Override
    public Boolean updateGroupInfo(GroupDto dto) {
        ImGroupPo existing = groupDubboService.queryOne(dto.getGroupId());
        if (existing == null) {
            throw new GroupException("群组不存在");
        }

        ImGroupPo update = new ImGroupPo().setGroupId(dto.getGroupId());
        if (StringUtils.hasText(dto.getGroupName())) {
            update.setGroupName(dto.getGroupName());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            update.setAvatar(dto.getAvatar());
        }
        if (StringUtils.hasText(dto.getIntroduction())) {
            update.setIntroduction(dto.getIntroduction());
        }
        if (StringUtils.hasText(dto.getNotification())) {
            update.setNotification(dto.getNotification());
        }

        if (!groupDubboService.modify(update)) {
            throw new GroupException("更新群信息失败");
        }

        log.info("更新群信息成功: groupId={}", dto.getGroupId());
        return true;
    }

    /**
     * 更新群成员信息
     *
     * @param dto 成员信息（已在 Controller 层校验）
     * @return 是否成功
     */
    @Override
    public Boolean updateGroupMember(GroupMemberDto dto) {
        ImGroupMemberPo member = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
        if (member == null) {
            throw new GroupException("用户不在群聊中");
        }

        ImGroupMemberPo update = new ImGroupMemberPo().setGroupMemberId(member.getGroupMemberId());
        if (StringUtils.hasText(dto.getAlias())) {
            update.setAlias(dto.getAlias());
        }
        if (StringUtils.hasText(dto.getRemark())) {
            update.setRemark(dto.getRemark());
        }

        if (!groupMemberDubboService.modify(update)) {
            throw new GroupException("更新群成员信息失败");
        }

        log.info("更新群成员信息成功: groupId={}, userId={}", dto.getGroupId(), dto.getUserId());
        return true;
    }

    // ==================== 私有方法 ====================

    /**
     * 创建群聊
     */
    private String createGroup(GroupInviteDto dto) {
        if (CollectionUtils.isEmpty(dto.getMemberIds())) {
            throw new GroupException("至少需要一个被邀请人");
        }

        String groupId = idDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_message_id).getStringId();
        String groupName = "默认群聊" + IdUtils.randomUUID().substring(0, 8);
        long now = DateTimeUtils.getCurrentUTCTimestamp();

        // 创建成员列表（包含群主）
        List<ImGroupMemberPo> members = new ArrayList<>();
        members.add(buildMemberPo(groupId, dto.getUserId(), IMemberStatus.GROUP_OWNER, now));
        dto.getMemberIds().forEach(id -> members.add(buildMemberPo(groupId, id, IMemberStatus.NORMAL, now)));

        // 批量插入成员
        if (!Boolean.TRUE.equals(groupMemberDubboService.creatBatch(members))) {
            throw new GroupException("群成员创建失败");
        }

        // 创建群
        ImGroupPo group = new ImGroupPo()
                .setGroupId(groupId)
                .setOwnerId(dto.getUserId())
                .setGroupType(IMGroupType.PRIVATE.getCode())
                .setGroupName(groupName)
                .setApplyJoinType(ImGroupJoinStatus.FREE.getCode())
                .setStatus(IMStatus.YES.getCode());

        if (!groupDubboService.creat(group)) {
            throw new GroupException("群信息创建失败");
        }

        // 异步生成群头像
        generateGroupAvatar(groupId);

        // 缓存群信息
        RMapCache<String, Object> cache = redissonClient.getMapCache(CACHE_GROUP_INFO);
        cache.fastPut(groupId, group, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

        // 发送欢迎消息
        messageService.sendGroupMessage(buildSystemMessage(groupId, "已加入群聊,请尽情聊天吧"));

        log.info("创建群聊成功: groupId={}, ownerId={}", groupId, dto.getUserId());
        return groupId;
    }

    /**
     * 邀请入群
     */
    private String inviteToGroup(GroupInviteDto dto) {
        String groupId = StringUtils.hasText(dto.getGroupId())
                ? dto.getGroupId()
                : idDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_message_id).getStringId();

        String lockKey = LOCK_PREFIX + "invite:" + groupId + ":" + dto.getUserId();
        return lockExecutor.execute(lockKey, () -> {
            // 获取现有成员
            List<ImGroupMemberPo> existingMembers = groupMemberDubboService.queryList(groupId);
            Set<String> existingIds = existingMembers.stream()
                    .map(ImGroupMemberPo::getMemberId)
                    .collect(Collectors.toSet());

            // 验证邀请者是否在群中
            if (!existingIds.contains(dto.getUserId())) {
                throw new GroupException("用户不在该群组中，不可邀请新成员");
            }

            // 过滤已在群中的用户
            List<String> inviteeIds = Optional.ofNullable(dto.getMemberIds()).orElse(Collections.emptyList());
            List<String> newInvitees = inviteeIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .distinct()
                    .toList();

            if (newInvitees.isEmpty()) {
                return groupId;
            }

            // 创建邀请请求
            long now = DateTimeUtils.getCurrentUTCTimestamp();
            long expireTime = now + 7L * 24 * 3600;
            ImGroupPo group = groupDubboService.queryOne(groupId);
            String verifierId = group != null ? group.getOwnerId() : dto.getUserId();

            List<ImGroupInviteRequestPo> requests = newInvitees.stream()
                    .map(toId -> buildInviteRequest(groupId, dto.getUserId(), toId, verifierId, dto.getMessage(), dto.getAddSource(), expireTime))
                    .toList();

            if (!Boolean.TRUE.equals(groupInviteRequestDubboService.creatBatch(requests))) {
                throw new GroupException("保存邀请请求失败");
            }

            // 发送邀请消息
            sendBatchInviteMessages(groupId, dto.getUserId(), newInvitees, group);

            log.info("群邀请发送成功: groupId={}, inviter={}, invitees={}", groupId, dto.getUserId(), newInvitees.size());
            return groupId;
        });
    }

    /**
     * 直接加入群聊
     */
    private String processDirectJoin(String groupId, String userId, String inviterId) {
        String lockKey = LOCK_PREFIX + "join:" + groupId + ":" + userId;
        return lockExecutor.execute(lockKey, () -> {
            ImGroupMemberPo member = groupMemberDubboService.queryOne(groupId, userId);
            if (member != null && IMemberStatus.NORMAL.getCode().equals(member.getRole())) {
                return "用户已加入群聊";
            }

            long now = DateTimeUtils.getCurrentUTCTimestamp();
            ImGroupMemberPo newMember = buildMemberPo(groupId, userId, IMemberStatus.NORMAL, now);
            if (!Boolean.TRUE.equals(groupMemberDubboService.creatBatch(List.of(newMember)))) {
                throw new GroupException("加入群聊失败");
            }

            // 更新群信息并通知
            updateGroupInfoAndNotify(groupId, inviterId, userId);

            log.info("用户加入群聊成功: groupId={}, userId={}", groupId, userId);
            return "成功加入群聊";
        });
    }

    /**
     * 更新群信息并发送通知
     */
    private void updateGroupInfoAndNotify(String groupId, String inviterId, String userId) {
        List<ImGroupMemberPo> updatedMembers = groupMemberDubboService.queryList(groupId);
        if (updatedMembers != null && updatedMembers.size() < 10) {
            groupDubboService.modify(new ImGroupPo().setGroupId(groupId));
            generateGroupAvatar(groupId);
        }
        sendJoinNotification(groupId, inviterId, userId);
    }

    /**
     * 生成群头像
     */
    public void generateGroupAvatar(String groupId) {
        try {
            List<String> avatars = groupMemberDubboService.queryNinePeopleAvatar(groupId);
        java.io.File headFile = groupHeadImageUtils.getCombinationOfhead(avatars, "defaultGroupHead" + groupId);
        FileVo fileVo = fileService.uploadFile(headFile);
        if (fileVo == null || !StringUtils.hasText(fileVo.getPath())) {
            return;
        }

        String avatarUrl = fileVo.getPath();
            groupDubboService.modify(new ImGroupPo().setGroupId(groupId).setAvatar(avatarUrl));

            // 更新缓存
            RMapCache<String, Object> cache = redissonClient.getMapCache(CACHE_GROUP_INFO);
        Object cached = cache.get(groupId);
        if (cached instanceof ImGroupPo cachedGroup) {
            cachedGroup.setAvatar(avatarUrl);
            cache.fastPut(groupId, cachedGroup, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        }
        } catch (Exception e) {
            log.error("生成群头像失败: groupId={}", groupId, e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取用户信息映射
     */
    private Map<String, ImUserDataPo> getUserMap(List<String> userIds) {
        List<ImUserDataPo> users = userDataDubboService.queryListByIds(userIds);
        return users.stream().collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));
    }

    /**
     * 构建群成员 VO
     */
    private GroupMemberVo buildGroupMemberVo(ImGroupMemberPo member, ImUserDataPo user) {
        GroupMemberVo vo = GroupMemberBeanMapper.INSTANCE.toGroupMemberVo(member);
        vo.setName(user.getName());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setLocation(user.getLocation());
        vo.setSelfSignature(user.getSelfSignature());
        vo.setBirthDay(user.getBirthday());
        vo.setRole(member.getRole());
        vo.setMute(member.getMute());
        vo.setAlias(member.getAlias());
        vo.setJoinType(member.getJoinType());
        return vo;
    }

    /**
     * 构建成员 PO
     */
    private ImGroupMemberPo buildMemberPo(String groupId, String memberId, IMemberStatus role, long joinTime) {
        return new ImGroupMemberPo()
                .setGroupId(groupId)
                .setGroupMemberId(IdUtils.snowflakeIdStr())
                .setMemberId(memberId)
                .setRole(role.getCode())
                .setMute(IMStatus.NO.getCode())
                .setJoinTime(joinTime);
    }

    /**
     * 构建邀请请求
     */
    private ImGroupInviteRequestPo buildInviteRequest(String groupId, String fromId, String toId,
                                                      String verifierId, String message, Integer addSource, long expireTime) {
        String requestId = idDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_invite_id).getStringId();
        return new ImGroupInviteRequestPo()
                .setRequestId(requestId)
                .setGroupId(groupId)
                .setFromId(fromId)
                .setToId(toId)
                .setVerifierId(verifierId)
                .setVerifierStatus(IMApproveStatus.PENDING.getCode())
                .setMessage(message)
                .setApproveStatus(IMApproveStatus.PENDING.getCode())
                .setAddSource(addSource)
                .setExpireTime(expireTime);
    }

    /**
     * 构建系统消息
     */
    private IMGroupMessage buildSystemMessage(String groupId, String message) {
        return IMGroupMessage.builder()
                .groupId(groupId)
                .fromId(IMConstant.SYSTEM)
                .messageContentType(IMessageContentType.TIP.getCode())
                .messageType(IMessageType.GROUP_MESSAGE.getCode())
                .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                .readStatus(IMessageReadStatus.UNREAD.getCode())
                .messageBody(new IMessage.TextMessageBody().setText(message))
                .build();
    }

    /**
     * 批量发送邀请消息
     */
    public void sendBatchInviteMessages(String groupId, String inviterId, List<String> invitees, ImGroupPo group) {
        ImUserDataPo inviterInfo = userDataDubboService.queryOne(inviterId);
        for (String inviteeId : invitees) {
            IMSingleMessage msg = IMSingleMessage.builder()
                    .messageTempId(IdUtils.snowflakeIdStr())
                    .fromId(inviterId)
                    .toId(inviteeId)
                    .messageContentType(IMessageContentType.GROUP_INVITE.getCode())
                    .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                    .messageType(IMessageType.SINGLE_MESSAGE.getCode())
                    .messageBody(new IMessage.GroupInviteMessageBody()
                    .setRequestId("")
                    .setGroupId(groupId)
                            .setUserId(inviteeId)
                            .setGroupAvatar(group != null ? group.getAvatar() : "")
                            .setGroupName(group != null ? group.getGroupName() : "")
                    .setInviterId(inviterId)
                    .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                            .setApproveStatus(IMApproveStatus.PENDING.getCode()))
                    .build();
            messageService.sendSingleMessage(msg);
        }
    }

    /**
     * 发送入群通知
     */
    public void sendJoinNotification(String groupId, String inviterId, String userId) {
        ImUserDataPo invitee = userDataDubboService.queryOne(userId);
        ImUserDataPo inviter = userDataDubboService.queryOne(inviterId);
        String message = "\"" + (inviter != null ? inviter.getName() : inviterId) + "\" 邀请 \"" +
                (invitee != null ? invitee.getName() : userId) + "\" 加入群聊";
        messageService.sendGroupMessage(buildSystemMessage(groupId, message));
    }

    /**
     * 发送入群审批请求给管理员
     */
    private void sendJoinApprovalRequest(String groupId, String inviterId, String inviteeId, ImGroupPo group) {
        List<ImGroupMemberPo> members = groupMemberDubboService.queryList(groupId);
        if (CollectionUtils.isEmpty(members)) {
            return;
        }

        // 获取管理员列表
        List<String> adminIds = members.stream()
                .filter(m -> IMemberStatus.GROUP_OWNER.getCode().equals(m.getRole())
                        || IMemberStatus.ADMIN.getCode().equals(m.getRole()))
                .map(ImGroupMemberPo::getMemberId)
                .distinct()
                .toList();

        if (adminIds.isEmpty() && group != null && StringUtils.hasText(group.getOwnerId())) {
            adminIds = List.of(group.getOwnerId());
        }

        if (adminIds.isEmpty()) {
            return;
        }

        ImUserDataPo inviterInfo = userDataDubboService.queryOne(inviterId);
        for (String adminId : adminIds) {
            IMSingleMessage msg = IMSingleMessage.builder()
                    .messageTempId(IdUtils.snowflakeIdStr())
                    .fromId(inviterId)
                    .toId(adminId)
                    .messageContentType(IMessageType.JOIN_APPROVE_GROUP.getCode())
                    .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                    .messageBody(new IMessage.GroupInviteMessageBody()
                    .setInviterId(inviterId)
                    .setGroupId(groupId)
                            .setUserId(inviteeId)
                            .setGroupAvatar(group != null ? group.getAvatar() : "")
                            .setGroupName(group != null ? group.getGroupName() : "")
                    .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                            .setApproveStatus(IMApproveStatus.PENDING.getCode()))
                    .build();
            messageService.sendSingleMessage(msg);
        }
    }

    // ==================== 群管理操作实现 ====================

    /**
     * 踢出群成员
     */
    @Override
    public void kickMember(GroupMemberDto dto) {
        String lockKey = LOCK_PREFIX + "kick:" + dto.getGroupId() + ":" + dto.getTargetUserId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者权限
            ImGroupMemberPo operator = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
            if (operator == null) {
                throw new GroupException("操作者不在群聊中");
            }

            // 只有群主和管理员可以踢人
            if (!IMemberStatus.GROUP_OWNER.getCode().equals(operator.getRole())
                    && !IMemberStatus.ADMIN.getCode().equals(operator.getRole())) {
                throw new GroupException("无权限执行此操作");
            }

            // 查找目标成员
            ImGroupMemberPo target = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getTargetUserId());
            if (target == null) {
                throw new GroupException("目标用户不在群聊中");
            }

            // 管理员不能踢群主和其他管理员
            if (IMemberStatus.ADMIN.getCode().equals(operator.getRole())) {
                if (IMemberStatus.GROUP_OWNER.getCode().equals(target.getRole())
                        || IMemberStatus.ADMIN.getCode().equals(target.getRole())) {
                    throw new GroupException("管理员无法移除群主或其他管理员");
                }
            }

            // 群主不能踢自己
            if (dto.getUserId().equals(dto.getTargetUserId())) {
                throw new GroupException("不能移除自己");
            }

            // 执行踢人
            if (!groupMemberDubboService.removeOne(target.getGroupMemberId())) {
                throw new GroupException("移除成员失败");
            }

            // 发送群操作消息
            sendGroupOperationWithTarget(dto.getGroupId(), IMessageType.KICK_FROM_GROUP,
                    dto.getUserId(), dto.getTargetUserId(), "被移出群聊");

            log.info("踢出群成员成功: groupId={}, operator={}, target={}",
                    dto.getGroupId(), dto.getUserId(), dto.getTargetUserId());
        });
    }

    /**
     * 设置/取消管理员
     */
    @Override
    public void setAdmin(GroupMemberDto dto) {
        String lockKey = LOCK_PREFIX + "admin:" + dto.getGroupId() + ":" + dto.getTargetUserId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者是否为群主
            ImGroupMemberPo operator = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
            if (operator == null || !IMemberStatus.GROUP_OWNER.getCode().equals(operator.getRole())) {
                throw new GroupException("只有群主可以设置管理员");
            }

            // 查找目标成员
            ImGroupMemberPo target = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getTargetUserId());
            if (target == null) {
                throw new GroupException("目标用户不在群聊中");
            }

            // 不能设置群主为管理员
            if (IMemberStatus.GROUP_OWNER.getCode().equals(target.getRole())) {
                throw new GroupException("不能更改群主的角色");
            }

            // 更新角色
            Integer oldRole = target.getRole();
            Integer newRole = IMemberStatus.ADMIN.getCode().equals(dto.getRole())
                    ? IMemberStatus.ADMIN.getCode()
                    : IMemberStatus.NORMAL.getCode();

            target.setRole(newRole);
            if (!groupMemberDubboService.modify(target)) {
                throw new GroupException("设置管理员失败");
            }

            // 发送群操作消息
            boolean isPromote = IMemberStatus.ADMIN.getCode().equals(newRole);
            IMessageType actionType = isPromote ? IMessageType.PROMOTE_TO_ADMIN : IMessageType.DEMOTE_FROM_ADMIN;
            String action = isPromote ? "被设为管理员" : "被取消管理员";
            Map<String, Object> extra = Map.of("newRole", newRole, "oldRole", oldRole);
            sendGroupOperationWithTarget(dto.getGroupId(), actionType,
                    dto.getUserId(), dto.getTargetUserId(), action, extra);

            log.info("设置管理员成功: groupId={}, target={}, role={}",
                    dto.getGroupId(), dto.getTargetUserId(), newRole);
        });
    }

    /**
     * 移交群主
     */
    @Override
    public void transferOwner(GroupMemberDto dto) {
        String lockKey = LOCK_PREFIX + "transfer:" + dto.getGroupId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者是否为当前群主
            ImGroupMemberPo currentOwner = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
            if (currentOwner == null || !IMemberStatus.GROUP_OWNER.getCode().equals(currentOwner.getRole())) {
                throw new GroupException("只有群主可以移交群主");
            }

            // 查找新群主
            ImGroupMemberPo newOwner = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getTargetUserId());
            if (newOwner == null) {
                throw new GroupException("目标用户不在群聊中");
            }

            // 不能移交给自己
            if (dto.getUserId().equals(dto.getTargetUserId())) {
                throw new GroupException("不能移交给自己");
            }

            // 更新原群主为普通成员
            currentOwner.setRole(IMemberStatus.NORMAL.getCode());
            if (!groupMemberDubboService.modify(currentOwner)) {
                throw new GroupException("更新原群主角色失败");
            }

            // 更新新群主
            newOwner.setRole(IMemberStatus.GROUP_OWNER.getCode());
            if (!groupMemberDubboService.modify(newOwner)) {
                // 回滚原群主角色
                currentOwner.setRole(IMemberStatus.GROUP_OWNER.getCode());
                groupMemberDubboService.modify(currentOwner);
                throw new GroupException("设置新群主失败");
            }

            // 更新群信息
            ImGroupPo group = groupDubboService.queryOne(dto.getGroupId());
            if (group != null) {
                group.setOwnerId(dto.getTargetUserId());
                groupDubboService.modify(group);
            }

            // 发送群操作消息
            Map<String, Object> extra = Map.of(
                    "oldOwner", dto.getUserId(),
                    "newOwner", dto.getTargetUserId()
            );
            sendGroupOperationWithTarget(dto.getGroupId(), IMessageType.TRANSFER_GROUP_OWNER,
                    dto.getUserId(), dto.getTargetUserId(), "成为新群主", extra);

            log.info("移交群主成功: groupId={}, oldOwner={}, newOwner={}",
                    dto.getGroupId(), dto.getUserId(), dto.getTargetUserId());
        });
    }

    /**
     * 设置群加入方式
     */
    @Override
    public void setJoinMode(GroupDto dto) {
        String lockKey = LOCK_PREFIX + "joinMode:" + dto.getGroupId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者权限
            validateAdminPermission(dto.getGroupId(), dto.getUserId());

            // 验证加入方式参数
            ImGroupJoinStatus joinStatus = ImGroupJoinStatus.getByCode(dto.getApplyJoinType());
            if (joinStatus == null) {
                throw new GroupException("无效的加入方式");
            }

            // 更新群设置
            ImGroupPo update = new ImGroupPo()
                    .setGroupId(dto.getGroupId())
                    .setApplyJoinType(dto.getApplyJoinType());

            if (!groupDubboService.modify(update)) {
                throw new GroupException("设置加入方式失败");
            }

            // 更新缓存
            invalidateGroupCache(dto.getGroupId());

            // 发送群操作消息
            String description = "群加入方式已设置为：" + joinStatus.getDesc();
            Map<String, Object> extra = Map.of("joinMode", dto.getApplyJoinType());
            sendGroupOperationWithoutTarget(dto.getGroupId(), IMessageType.SET_GROUP_JOIN_MODE,
                    dto.getUserId(), description, extra);

            log.info("设置群加入方式成功: groupId={}, joinType={}",
                    dto.getGroupId(), joinStatus.getDesc());
        });
    }

    /**
     * 禁言/取消禁言成员
     */
    @Override
    public void muteMember(GroupMemberDto dto) {
        String lockKey = LOCK_PREFIX + "mute:" + dto.getGroupId() + ":" + dto.getTargetUserId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者权限
            ImGroupMemberPo operator = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
            if (operator == null) {
                throw new GroupException("操作者不在群聊中");
            }

            if (!IMemberStatus.GROUP_OWNER.getCode().equals(operator.getRole())
                    && !IMemberStatus.ADMIN.getCode().equals(operator.getRole())) {
                throw new GroupException("无权限执行此操作");
            }

            // 查找目标成员
            ImGroupMemberPo target = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getTargetUserId());
            if (target == null) {
                throw new GroupException("目标用户不在群聊中");
            }

            // 管理员不能禁言群主和其他管理员
            if (IMemberStatus.ADMIN.getCode().equals(operator.getRole())) {
                if (IMemberStatus.GROUP_OWNER.getCode().equals(target.getRole())
                        || IMemberStatus.ADMIN.getCode().equals(target.getRole())) {
                    throw new GroupException("无法禁言群主或其他管理员");
                }
            }

            // 更新禁言状态
            target.setMute(dto.getMute());
            if (!groupMemberDubboService.modify(target)) {
                throw new GroupException("更新禁言状态失败");
            }

            // 发送群操作消息
            boolean isMute = IMStatus.NO.getCode().equals(dto.getMute());
            IMessageType actionType = isMute ? IMessageType.MUTE_MEMBER : IMessageType.UNMUTE_MEMBER;
            String action = isMute ? "被禁言" : "被解除禁言";
            Map<String, Object> extra = new HashMap<>();
            extra.put("mute", dto.getMute());
            if (dto.getMuteDuration() != null) {
                extra.put("muteDuration", dto.getMuteDuration());
            }
            sendGroupOperationWithTarget(dto.getGroupId(), actionType,
                    dto.getUserId(), dto.getTargetUserId(), action, extra);

            log.info("禁言成员操作成功: groupId={}, target={}, mute={}",
                    dto.getGroupId(), dto.getTargetUserId(), dto.getMute());
        });
    }

    /**
     * 全员禁言/取消全员禁言
     */
    @Override
    public void muteAll(GroupDto dto) {
        String lockKey = LOCK_PREFIX + "muteAll:" + dto.getGroupId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者权限
            validateAdminPermission(dto.getGroupId(), dto.getUserId());

            // 更新群设置
            ImGroupPo update = new ImGroupPo()
                    .setGroupId(dto.getGroupId())
                    .setMute(dto.getMuteAll());

            if (!groupDubboService.modify(update)) {
                throw new GroupException("设置全员禁言失败");
            }

            // 更新缓存
            invalidateGroupCache(dto.getGroupId());

            // 发送群操作消息
            boolean isMuteAll = IMStatus.NO.getCode().equals(dto.getMuteAll());
            IMessageType actionType = isMuteAll ? IMessageType.MUTE_ALL : IMessageType.UNMUTE_ALL;
            String description = isMuteAll ? "群主/管理员开启了全员禁言" : "群主/管理员关闭了全员禁言";
            Map<String, Object> extra = Map.of("muteAll", dto.getMuteAll());
            sendGroupOperationWithoutTarget(dto.getGroupId(), actionType, dto.getUserId(), description, extra);

            log.info("全员禁言操作成功: groupId={}, muteAll={}", dto.getGroupId(), dto.getMuteAll());
        });
    }

    /**
     * 解散群组
     */
    @Override
    public void dismissGroup(GroupDto dto) {
        String lockKey = LOCK_PREFIX + "dismiss:" + dto.getGroupId();
        lockExecutor.execute(lockKey, () -> {
            // 只有群主可以解散群
            ImGroupMemberPo operator = groupMemberDubboService.queryOne(dto.getGroupId(), dto.getUserId());
            if (operator == null || !IMemberStatus.GROUP_OWNER.getCode().equals(operator.getRole())) {
                throw new GroupException("只有群主可以解散群组");
            }

            // 发送解散通知（在删除成员前发送，确保所有成员能收到）
            sendGroupOperationWithoutTarget(dto.getGroupId(), IMessageType.REMOVE_GROUP,
                    dto.getUserId(), "群组已被群主解散", null);

            // 获取所有成员并删除
            List<ImGroupMemberPo> members = groupMemberDubboService.queryList(dto.getGroupId());
            for (ImGroupMemberPo member : members) {
                groupMemberDubboService.removeOne(member.getGroupMemberId());
            }

            // 更新群状态为已解散
            ImGroupPo update = new ImGroupPo()
                    .setGroupId(dto.getGroupId())
                    .setStatus(IMStatus.NO.getCode());
            groupDubboService.modify(update);

            // 清除缓存
            invalidateGroupCache(dto.getGroupId());

            log.info("解散群组成功: groupId={}, operator={}", dto.getGroupId(), dto.getUserId());
        });
    }

    /**
     * 设置群公告
     */
    @Override
    public void setAnnouncement(GroupDto dto) {
        String lockKey = LOCK_PREFIX + "announcement:" + dto.getGroupId();
        lockExecutor.execute(lockKey, () -> {
            // 验证操作者权限
            validateAdminPermission(dto.getGroupId(), dto.getUserId());

            // 更新群公告
            ImGroupPo update = new ImGroupPo()
                    .setGroupId(dto.getGroupId())
                    .setNotification(dto.getNotification());

            if (!groupDubboService.modify(update)) {
                throw new GroupException("设置群公告失败");
            }

            // 更新缓存
            invalidateGroupCache(dto.getGroupId());

            // 发送群操作消息
            String description = "群公告已更新：" + dto.getNotification();
            Map<String, Object> extra = Map.of("announcement", dto.getNotification());
            sendGroupOperationWithoutTarget(dto.getGroupId(), IMessageType.SET_GROUP_ANNOUNCEMENT,
                    dto.getUserId(), description, extra);

            log.info("设置群公告成功: groupId={}", dto.getGroupId());
        });
    }

    // ==================== 群管理辅助方法 ====================

    /**
     * 验证管理员权限
     */
    private void validateAdminPermission(String groupId, String userId) {
        ImGroupMemberPo operator = groupMemberDubboService.queryOne(groupId, userId);
        if (operator == null) {
            throw new GroupException("操作者不在群聊中");
        }

        if (!IMemberStatus.GROUP_OWNER.getCode().equals(operator.getRole())
                && !IMemberStatus.ADMIN.getCode().equals(operator.getRole())) {
            throw new GroupException("无权限执行此操作");
        }
    }

    /**
     * 发送系统通知消息（纯文本形式，兼容旧逻辑）
     */
    private void sendSystemNotification(String groupId, String operatorId, String targetId, String action) {
        ImUserDataPo operator = userDataDubboService.queryOne(operatorId);
        ImUserDataPo target = userDataDubboService.queryOne(targetId);
        String operatorName = operator != null ? operator.getName() : operatorId;
        String targetName = target != null ? target.getName() : targetId;
        String message = "\"" + operatorName + "\" 将 \"" + targetName + "\" " + action;
        messageService.sendGroupMessage(buildSystemMessage(groupId, message));
    }

    /**
     * 发送群操作通知消息（结构化消息体）
     *
     * @param groupId       群组ID
     * @param operationType 操作类型（IMessageType 200-299 区间的 code）
     * @param operatorId    操作者ID
     * @param targetId      目标用户ID（可为 null）
     * @param description   操作描述
     * @param extra         扩展数据
     */
    private void sendGroupOperationMessage(String groupId, IMessageType operationType,
                                           String operatorId, String targetId,
                                           String description, Map<String, Object> extra) {
        // 获取群信息
        ImGroupPo group = groupDubboService.queryOne(groupId);

        // 获取操作者信息
        ImUserDataPo operatorInfo = userDataDubboService.queryOne(operatorId);

        // 获取目标用户信息（如有）
        ImUserDataPo targetInfo = StringUtils.hasText(targetId) ? userDataDubboService.queryOne(targetId) : null;

        // 构建群操作消息体
        IMessage.GroupOperationMessageBody body = IMessage.GroupOperationMessageBody.builder()
                .operationType(operationType.getCode())
                .groupId(groupId)
                .groupName(group != null ? group.getGroupName() : null)
                .groupAvatar(group != null ? group.getAvatar() : null)
                .operatorId(operatorId)
                .operatorName(operatorInfo != null ? operatorInfo.getName() : operatorId)
                .targetUserId(targetId)
                .targetUserName(targetInfo != null ? targetInfo.getName() : targetId)
                .operationTime(DateTimeUtils.getCurrentUTCTimestamp())
                .description(description)
                .extra(extra)
                .build();

        // 构建群消息
        IMGroupMessage message = IMGroupMessage.builder()
                .groupId(groupId)
                .fromId(IMConstant.SYSTEM)
                .messageContentType(IMessageContentType.GROUP_OPERATION.getCode())
                .messageType(IMessageType.GROUP_MESSAGE.getCode())
                .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                .readStatus(IMessageReadStatus.UNREAD.getCode())
                .messageBody(body)
                .build();

        messageService.sendGroupMessage(message);
    }

    /**
     * 发送带目标用户的群操作通知（便捷方法）
     */
    private void sendGroupOperationWithTarget(String groupId, IMessageType operationType,
                                              String operatorId, String targetId, String action) {
        sendGroupOperationWithTarget(groupId, operationType, operatorId, targetId, action, null);
    }

    /**
     * 发送带目标用户的群操作通知（带扩展数据）
     */
    private void sendGroupOperationWithTarget(String groupId, IMessageType operationType,
                                              String operatorId, String targetId,
                                              String action, Map<String, Object> extra) {
        ImUserDataPo operatorInfo = userDataDubboService.queryOne(operatorId);
        ImUserDataPo targetInfo = userDataDubboService.queryOne(targetId);
        String operatorName = operatorInfo != null ? operatorInfo.getName() : operatorId;
        String targetName = targetInfo != null ? targetInfo.getName() : targetId;
        String description = "\"" + operatorName + "\" 将 \"" + targetName + "\" " + action;

        sendGroupOperationMessage(groupId, operationType, operatorId, targetId, description, extra);
    }

    /**
     * 发送无目标用户的群操作通知（如全员禁言、解散群等）
     */
    private void sendGroupOperationWithoutTarget(String groupId, IMessageType operationType,
                                                 String operatorId, String description,
                                                 Map<String, Object> extra) {
        sendGroupOperationMessage(groupId, operationType, operatorId, null, description, extra);
    }

    /**
     * 使群缓存失效
     */
    private void invalidateGroupCache(String groupId) {
        RMapCache<String, Object> cache = redissonClient.getMapCache(CACHE_GROUP_INFO);
        cache.fastRemove(groupId);
    }
}
