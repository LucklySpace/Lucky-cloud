package com.xy.lucky.server.service.impl;

import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.*;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMessage;
import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import com.xy.lucky.domain.mapper.GroupMemberBeanMapper;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.vo.FileVo;
import com.xy.lucky.domain.vo.GroupMemberVo;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupInviteRequestDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.web.api.id.ImIdDubboService;
import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.server.config.IdGeneratorConstant;
import com.xy.lucky.server.exception.GroupException;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.FileService;
import com.xy.lucky.server.service.GroupService;
import com.xy.lucky.server.service.MessageService;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.image.GroupHeadImageUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {


    static final String GROUP_INFO_PREFIX = "group:info:";
    static final String GROUP_MEMBERS_PREFIX = "group:members:";
    static final long TTL_SECONDS = 300L;
    static final String JOIN_PREFIX = "lock:join:";
    static final String INVITE_PREFIX = "lock:invite:";
    static final String QUIT_PREFIX = "lock:quit:";
    static final long WAIT_TIME = 5L;
    static final long LEASE_TIME = 10L;

    private final GroupHeadImageUtils groupHeadImageUtils = new GroupHeadImageUtils();

    @DubboReference
    private ImUserDataDubboService imUserDataDubboService;

    @DubboReference
    private ImGroupDubboService imGroupDubboService;

    @DubboReference
    private ImGroupMemberDubboService imGroupMemberDubboService;

    @DubboReference
    private ImGroupInviteRequestDubboService imGroupInviteRequestDubboService;

    @DubboReference
    private ImIdDubboService imIdDubboService;

    @Resource
    private MessageService messageService;

    @Resource
    private FileService fileService;

    @Resource
    private RedissonClient redissonClient;

    private <T> T withLockSync(String key, String logDesc, ThrowingSupplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS);
            if (!acquired) {
                throw new MessageException("无法获取锁: " + logDesc);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageException("无法获取锁: " + logDesc);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (acquired && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public Mono<Map<?, ?>> getGroupMembers(GroupDto groupDto) {
        return Mono.fromCallable(() -> {
            String groupId = groupDto.getGroupId();
            List<ImGroupMemberPo> members = imGroupMemberDubboService.queryList(groupId);
            if (CollectionUtils.isEmpty(members)) {
                return Collections.emptyMap();
            }
            List<String> memberIds = members.stream()
                    .map(ImGroupMemberPo::getMemberId)
                    .collect(Collectors.toList());
            List<ImUserDataPo> users = imUserDataDubboService.queryListByIds(memberIds);
            Map<String, ImUserDataPo> userMap = users.stream()
                    .collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));
            Map<String, GroupMemberVo> voMap = new HashMap<>(members.size());
            for (ImGroupMemberPo member : members) {
                ImUserDataPo user = userMap.get(member.getMemberId());
                if (user != null) {
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
                    voMap.put(user.getUserId(), vo);
                }
            }
            return voMap;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> quitGroup(GroupDto groupDto) {
        return Mono.fromCallable(() -> {
                    String groupId = groupDto.getGroupId();
                    String userId = groupDto.getUserId();
                    withLockSync(QUIT_PREFIX + groupId + ":" + userId, "退出群聊 " + groupId, () -> {
                        ImGroupMemberPo member = imGroupMemberDubboService.queryOne(groupId, userId);
                        if (member == null) {
                            throw new GroupException("用户不在群聊中");
                        }
                        if (IMemberStatus.GROUP_OWNER.getCode().equals(member.getRole())) {
                            throw new GroupException("群主不可退出群聊");
                        }
                        boolean success = imGroupMemberDubboService.removeOne(member.getGroupMemberId());
                        if (success) {
                            log.info("退出群聊成功 groupId={} userId={}", groupId, userId);
                        }
                        return null;
                    });
                    return 0;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<String> createGroup(@NonNull GroupInviteDto dto) {
        return Mono.fromCallable(() -> createGroupSync(dto))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> groupInvite(@NonNull GroupInviteDto dto) {
        return Mono.fromCallable(() -> groupInviteSync(dto))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> inviteGroup(GroupInviteDto dto) {
        return Mono.fromCallable(() -> {
                    Integer type = dto.getType();
                    if (IMessageType.CREATE_GROUP.getCode().equals(type)) {
                        return createGroupSync(dto);
                    } else if (IMessageType.GROUP_INVITE.getCode().equals(type)) {
                        return groupInviteSync(dto);
                    }
                    throw new GroupException("无效邀请类型");
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<String> approveGroupInvite(GroupInviteDto dto) {
        return Mono.fromCallable(() -> approveGroupInviteSync(dto))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ImGroupPo> groupInfo(@NonNull GroupDto groupDto) {
        return Mono.fromCallable(() -> {
                    String groupId = groupDto.getGroupId();
                    RMapCache<String, Object> cache = redissonClient.getMapCache(GROUP_INFO_PREFIX);
                    Object cached = cache.get(groupId);
                    if (cached instanceof ImGroupPo cachedGroup) {
                        return cachedGroup;
                    }

                    ImGroupPo group = imGroupDubboService.queryOne(groupId);
                    if (group != null) {
                        cache.fastPut(groupId, group, TTL_SECONDS, TimeUnit.SECONDS);
                        return group;
                    }
                    return new ImGroupPo();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> updateGroupInfo(GroupDto groupDto) {
        return Mono.fromCallable(() -> {
            String groupId = groupDto.getGroupId();
            ImGroupPo existingGroup = imGroupDubboService.queryOne(groupId);
            if (existingGroup == null) {
                throw new GroupException("群组不存在");
            }
            ImGroupPo updateGroup = new ImGroupPo().setGroupId(groupId);
            if (StringUtils.hasText(groupDto.getGroupName())) {
                updateGroup.setGroupName(groupDto.getGroupName());
            }
            if (StringUtils.hasText(groupDto.getAvatar())) {
                updateGroup.setAvatar(groupDto.getAvatar());
            }
            if (StringUtils.hasText(groupDto.getIntroduction())) {
                updateGroup.setIntroduction(groupDto.getIntroduction());
            }
            if (StringUtils.hasText(groupDto.getNotification())) {
                updateGroup.setNotification(groupDto.getNotification());
            }
            boolean success = imGroupDubboService.modify(updateGroup);
            if (!success) {
                throw new GroupException("更新失败");
            }
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Boolean> updateGroupMember(GroupMemberDto groupMemberDto) {
        return Mono.fromCallable(() -> {
            String groupId = groupMemberDto.getGroupId();
            String userId = groupMemberDto.getUserId();
            ImGroupMemberPo member = imGroupMemberDubboService.queryOne(groupId, userId);
            if (member == null) {
                throw new GroupException("用户不在群聊中");
            }
            ImGroupMemberPo updateMember = new ImGroupMemberPo()
                    .setGroupMemberId(member.getGroupMemberId());
            if (StringUtils.hasText(groupMemberDto.getAlias())) {
                updateMember.setAlias(groupMemberDto.getAlias());
            }
            if (StringUtils.hasText(groupMemberDto.getRemark())) {
                updateMember.setRemark(groupMemberDto.getRemark());
            }
            boolean success = imGroupMemberDubboService.modify(updateMember);
            if (!success) {
                throw new GroupException("更新失败");
            }
            return true;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private ImGroupMemberPo buildMember(String groupId, String memberId, IMemberStatus role, long joinTime) {
        return new ImGroupMemberPo()
                .setGroupId(groupId)
                .setGroupMemberId(IdUtils.snowflakeIdStr())
                .setMemberId(memberId)
                .setRole(role.getCode())
                .setMute(IMStatus.NO.getCode())
                .setDelFlag(IMStatus.YES.getCode())
                .setJoinTime(joinTime);
    }

    private IMGroupMessage systemMessage(String groupId, String message) {
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

    private String createGroupSync(@NonNull GroupInviteDto dto) {
        if (CollectionUtils.isEmpty(dto.getMemberIds())) {
            throw new GroupException("至少需要一个被邀请人");
        }

        String groupId = imIdDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_message_id).getStringId();
        String groupName = "默认群聊" + IdUtils.randomUUID();
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        String ownerId = dto.getUserId();
        List<String> memberIds = dto.getMemberIds();

        List<ImGroupMemberPo> members = new ArrayList<>(memberIds.size() + 1);
        members.add(buildMember(groupId, ownerId, IMemberStatus.GROUP_OWNER, now));
        memberIds.forEach(id -> members.add(buildMember(groupId, id, IMemberStatus.NORMAL, now)));

        boolean membersOk = Boolean.TRUE.equals(imGroupMemberDubboService.creatBatch(members));
        if (!membersOk) {
            throw new GroupException("群成员插入失败");
        }

        ImGroupPo group = new ImGroupPo()
                .setGroupId(groupId)
                .setOwnerId(ownerId)
                .setGroupType(1)
                .setGroupName(groupName)
                .setApplyJoinType(ImGroupJoinStatus.FREE.getCode())
                .setStatus(IMStatus.YES.getCode())
                .setCreateTime(now)
                .setDelFlag(IMStatus.YES.getCode());

        boolean groupOk = imGroupDubboService.creat(group);
        if (!groupOk) {
            throw new GroupException("群信息插入失败");
        }

        try {
            generateGroupAvatarSync(groupId);
        } catch (Exception e) {
            log.error("生成群头像失败", e);
        }

        RMapCache<String, Object> groupCache = redissonClient.getMapCache(GROUP_INFO_PREFIX);
        groupCache.fastPut(groupId, group, TTL_SECONDS, TimeUnit.SECONDS);

        messageService.sendGroupMessage(systemMessage(groupId, "已加入群聊,请尽情聊天吧")).block();
        return groupId;
    }

    private String groupInviteSync(@NonNull GroupInviteDto dto) {
        String gid = StringUtils.hasText(dto.getGroupId())
                ? dto.getGroupId()
                : imIdDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_message_id).getStringId();

        String inviterId = dto.getUserId();
        return withLockSync(INVITE_PREFIX + gid + ":" + inviterId, "群邀请 " + gid, () -> {
            List<ImGroupMemberPo> existingMembers = imGroupMemberDubboService.queryList(gid);
            Set<String> existingIds = existingMembers.stream()
                    .map(ImGroupMemberPo::getMemberId)
                    .collect(Collectors.toSet());

            if (!existingIds.contains(inviterId)) {
                throw new GroupException("用户不在该群组中，不可邀请新成员");
            }

            List<String> inviteeIds = Optional.ofNullable(dto.getMemberIds()).orElse(Collections.emptyList());
            List<String> newInvitees = inviteeIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .distinct()
                    .collect(Collectors.toList());

            if (newInvitees.isEmpty()) {
                return gid;
            }

            long now = DateTimeUtils.getCurrentUTCTimestamp();
            long expireTime = now + 7L * 24 * 3600;

            ImGroupPo groupPo = imGroupDubboService.queryOne(gid);
            String verifierId = groupPo != null ? groupPo.getOwnerId() : inviterId;

            List<ImGroupInviteRequestPo> requests = new ArrayList<>(newInvitees.size());
            for (String toId : newInvitees) {
                String requestId = imIdDubboService.generateId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_invite_id).getStringId();
                ImGroupInviteRequestPo po = new ImGroupInviteRequestPo()
                        .setRequestId(requestId)
                        .setGroupId(gid)
                        .setFromId(inviterId)
                        .setToId(toId)
                        .setVerifierId(verifierId)
                        .setVerifierStatus(0)
                        .setMessage(dto.getMessage())
                        .setApproveStatus(0)
                        .setAddSource(dto.getAddSource())
                        .setExpireTime(expireTime)
                        .setCreateTime(now)
                        .setDelFlag(1);
                requests.add(po);
            }

            Boolean dbOk = imGroupInviteRequestDubboService.creatBatch(requests);
            if (!Boolean.TRUE.equals(dbOk)) {
                throw new GlobalException(ResultCode.FAIL, "保存邀请请求失败");
            }

            sendBatchInviteMessagesSync(gid, inviterId, newInvitees, groupPo);
            return gid;
        });
    }

    private String approveGroupInviteSync(GroupInviteDto dto) {
        String groupId = dto.getGroupId();
        String userId = dto.getUserId();
        Integer approveStatus = dto.getApproveStatus();
        if (!StringUtils.hasText(groupId) || !StringUtils.hasText(userId) || approveStatus == null) {
            return "信息不完整";
        }
        if (approveStatus.equals(0)) {
            return "待处理群聊邀请";
        }
        if (approveStatus.equals(2)) {
            return "已拒绝群聊邀请";
        }

        ImGroupPo groupPo = imGroupDubboService.queryOne(groupId);
        if (groupPo == null) return "群不存在";
        if (ImGroupJoinStatus.BAN.getCode().equals(groupPo.getApplyJoinType())) {
            return "群不允许加入";
        }
        if (ImGroupJoinStatus.APPROVE.getCode().equals(groupPo.getApplyJoinType())) {
            sendJoinApprovalRequestToAdminsSync(groupId, dto.getInviterId(), dto.getUserId(), groupPo);
            return "已发送入群验证请求，等待审核";
        }
        return processDirectJoinSync(groupId, dto.getUserId(), dto.getInviterId());
    }

    private String processDirectJoinSync(String groupId, String userId, String inviterId) {
        return withLockSync(JOIN_PREFIX + groupId + ":" + userId, "加入群聊 " + groupId, () -> {
            ImGroupMemberPo member = imGroupMemberDubboService.queryOne(groupId, userId);
            if (member != null && IMemberStatus.NORMAL.getCode().equals(member.getRole())) {
                return "用户已加入群聊";
            }

            long now = DateTimeUtils.getCurrentUTCTimestamp();
            ImGroupMemberPo newMember = buildMember(groupId, userId, IMemberStatus.NORMAL, now);
            Boolean ok = imGroupMemberDubboService.creatBatch(List.of(newMember));
            if (Boolean.TRUE.equals(ok)) {
                updateGroupInfoAndNotifySync(groupId, inviterId, userId);
                return "成功加入群聊";
            }
            return "加入群聊失败";
        });
    }

    private void updateGroupInfoAndNotifySync(String groupId, String inviterId, String userId) {
        List<ImGroupMemberPo> updatedMembers = imGroupMemberDubboService.queryList(groupId);
        if (updatedMembers != null && updatedMembers.size() < 10) {
            imGroupDubboService.modify(new ImGroupPo().setGroupId(groupId));
            try {
                generateGroupAvatarSync(groupId);
            } catch (Exception e) {
                log.error("生成群头像失败", e);
            }
        }
        sendJoinNotificationSync(groupId, inviterId, userId);
    }

    private void generateGroupAvatarSync(String groupId) {
        List<String> avatars = imGroupMemberDubboService.queryNinePeopleAvatar(groupId);
        java.io.File headFile = groupHeadImageUtils.getCombinationOfhead(avatars, "defaultGroupHead" + groupId);
        FileVo fileVo = fileService.uploadFile(headFile).block();
        if (fileVo == null || !StringUtils.hasText(fileVo.getPath())) {
            return;
        }

        String avatarUrl = fileVo.getPath();
        imGroupDubboService.modify(new ImGroupPo().setGroupId(groupId).setAvatar(avatarUrl));

        RMapCache<String, Object> cache = redissonClient.getMapCache(GROUP_INFO_PREFIX);
        Object cached = cache.get(groupId);
        if (cached instanceof ImGroupPo cachedGroup) {
            cachedGroup.setAvatar(avatarUrl);
            cache.fastPut(groupId, cachedGroup, TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void sendBatchInviteMessagesSync(String groupId, String inviterId, List<String> invitees, ImGroupPo groupPo) {
        ImUserDataPo inviterInfo = imUserDataDubboService.queryOne(inviterId);
        for (String inviteeId : invitees) {
            IMSingleMessage msg = IMSingleMessage.builder()
                    .messageTempId(IdUtils.snowflakeIdStr())
                    .fromId(inviterId)
                    .toId(inviteeId)
                    .messageContentType(IMessageContentType.GROUP_INVITE.getCode())
                    .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                    .messageType(IMessageType.SINGLE_MESSAGE.getCode())
                    .build();

            IMessage.MessageBody body = new IMessage.GroupInviteMessageBody()
                    .setRequestId("")
                    .setGroupId(groupId)
                    .setUserId(inviteeId)
                    .setGroupAvatar(groupPo != null ? groupPo.getAvatar() : "")
                    .setGroupName(groupPo != null ? groupPo.getGroupName() : "")
                    .setInviterId(inviterId)
                    .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                    .setApproveStatus(0);
            msg.setMessageBody(body);
            messageService.sendSingleMessage(msg).block();
        }
    }

    private void sendJoinNotificationSync(String groupId, String inviterId, String userId) {
        ImUserDataPo invitee = imUserDataDubboService.queryOne(userId);
        ImUserDataPo inviter = imUserDataDubboService.queryOne(inviterId);
        String msg = "\"" + (inviter != null ? inviter.getName() : inviterId) + "\" 邀请 \"" +
                (invitee != null ? invitee.getName() : userId) + "\" 加入群聊";
        messageService.sendGroupMessage(systemMessage(groupId, msg)).block();
    }

    private void sendJoinApprovalRequestToAdminsSync(String groupId, String inviterId, String inviteeId, ImGroupPo groupPo) {
        List<ImGroupMemberPo> members = imGroupMemberDubboService.queryList(groupId);
        if (CollectionUtils.isEmpty(members)) return;

        List<String> adminIds = members.stream()
                .filter(m -> IMemberStatus.GROUP_OWNER.getCode().equals(m.getRole()) ||
                        IMemberStatus.ADMIN.getCode().equals(m.getRole()))
                .map(ImGroupMemberPo::getMemberId)
                .distinct()
                .collect(Collectors.toList());
        if (adminIds.isEmpty() && groupPo != null && StringUtils.hasText(groupPo.getOwnerId())) {
            adminIds = List.of(groupPo.getOwnerId());
        }
        if (adminIds.isEmpty()) return;

        ImUserDataPo inviterInfo = imUserDataDubboService.queryOne(inviterId);
        for (String adminId : adminIds) {
            IMSingleMessage msg = IMSingleMessage.builder()
                    .messageTempId(IdUtils.snowflakeIdStr())
                    .fromId(inviterId)
                    .toId(adminId)
                    .messageContentType(IMessageContentType.GROUP_JOIN_APPROVE.getCode())
                    .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                    .build();

            IMessage.MessageBody body = new IMessage.GroupInviteMessageBody()
                    .setInviterId(inviterId)
                    .setGroupId(groupId)
                    .setUserId(inviteeId)
                    .setGroupAvatar(groupPo != null ? groupPo.getAvatar() : "")
                    .setGroupName(groupPo != null ? groupPo.getGroupName() : "")
                    .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                    .setApproveStatus(0);
            msg.setMessageBody(body);
            messageService.sendSingleMessage(msg).block();
        }
    }

    public Mono<Void> generateGroupAvatar(String groupId) {
        return Mono.fromCallable(() -> {
                    try {
                        generateGroupAvatarSync(groupId);
                    } catch (Exception e) {
                        log.error("异步生成群头像失败 groupId={}", groupId, e);
                    }
                    return 0;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> sendBatchInviteMessages(String groupId, String inviterId, List<String> invitees, ImGroupPo groupPo) {
        return Mono.fromCallable(() -> {
                    sendBatchInviteMessagesSync(groupId, inviterId, invitees, groupPo);
                    return 0;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> sendJoinNotification(String groupId, String inviterId, String userId) {
        return Mono.fromCallable(() -> {
                    sendJoinNotificationSync(groupId, inviterId, userId);
                    return 0;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<Void> sendJoinApprovalRequestToAdmins(String groupId, String inviterId, String inviteeId, ImGroupPo groupPo) {
        return Mono.fromCallable(() -> {
                    sendJoinApprovalRequestToAdminsSync(groupId, inviterId, inviteeId, groupPo);
                    return 0;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
