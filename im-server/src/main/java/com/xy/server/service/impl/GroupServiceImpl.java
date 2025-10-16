package com.xy.server.service.impl;


import com.xy.core.constants.IMConstant;
import com.xy.core.enums.*;
import com.xy.core.model.IMGroupMessage;
import com.xy.core.model.IMSingleMessage;
import com.xy.core.model.IMessage;
import com.xy.domain.dto.GroupDto;
import com.xy.domain.dto.GroupInviteDto;
import com.xy.domain.po.ImGroupInviteRequestPo;
import com.xy.domain.po.ImGroupMemberPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.GroupMemberVo;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.api.IdGeneratorConstant;
import com.xy.server.api.feign.database.group.ImGroupFeign;
import com.xy.server.api.feign.database.user.ImUserFeign;
import com.xy.server.api.feign.id.ImIdGeneratorFeign;
import com.xy.server.exception.GlobalException;
import com.xy.server.service.FileService;
import com.xy.server.service.GroupService;
import com.xy.server.service.MessageService;
import com.xy.utils.DateTimeUtil;
import com.xy.utils.GroupHeadImageUtil;
import com.xy.utils.IdUtils;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    private static final String GROUP_CACHE_PREFIX = "group:info:";
    private static final String GROUP_MEMBERS_PREFIX = "group:members:";
    private static final String GROUP_JOIN_LOCK_PREFIX = "lock:join:";
    private static final String GROUP_INVITE_LOCK_PREFIX = "lock:invite:";
    private static final String GROUP_QUIT_LOCK_PREFIX = "lock:quit:";
    private static final long CACHE_TTL_SECONDS = 300L; // 5min TTL
    private static final long LOCK_WAIT_TIME = 5L; // 锁等待5s
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有10s
    private final GroupHeadImageUtil groupHeadImageUtil = new GroupHeadImageUtil();
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ImGroupFeign imGroupFeign;
    @Resource
    private ImUserFeign imUserFeign;
    @Resource
    private ImIdGeneratorFeign imIdGeneratorFeign;
    @Resource
    private FileService fileService;
    @Resource
    private MessageService messageService;
    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    /**
     * 获取群成员信息（Redisson RMapCache缓存）
     */
    @Override
    public Result<?> getGroupMembers(GroupDto groupDto) {
        long startTime = System.currentTimeMillis();
        try {
            String groupId = groupDto.getGroupId();
            RMapCache<String, Object> cache = redissonClient.getMapCache(GROUP_MEMBERS_PREFIX);
            Object cachedMembers = cache.get(groupId);
            List<ImGroupMemberPo> members;
            if (cachedMembers != null) {
                members = (List<ImGroupMemberPo>) cachedMembers;
                log.debug("缓存命中群成员 groupId={}", groupId);
            } else {
                members = imGroupFeign.getGroupMemberList(groupId);
                if (!CollectionUtils.isEmpty(members)) {
                    cache.fastPut(groupId, members, CACHE_TTL_SECONDS, TimeUnit.SECONDS); // 原子put + TTL
                }
                log.debug("缓存未命中，DB查询群成员 groupId={}", groupId);
            }

            if (CollectionUtils.isEmpty(members)) {
                return Result.success(Collections.emptyMap());
            }

            // 批量查询用户并构建VO
            List<String> memberIds = members.stream().map(ImGroupMemberPo::getMemberId).collect(Collectors.toList());
            List<ImUserDataPo> users = imUserFeign.getUserByIds(memberIds);
            Map<String, ImUserDataPo> userMap = users.stream().collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));

            Map<String, GroupMemberVo> voMap = new HashMap<>(members.size());
            for (ImGroupMemberPo member : members) {
                ImUserDataPo user = userMap.get(member.getMemberId());
                if (user != null) {
                    GroupMemberVo vo = new GroupMemberVo();
                    BeanUtils.copyProperties(user, vo);
                    vo.setRole(member.getRole());
                    vo.setMute(member.getMute());
                    vo.setAlias(member.getAlias());
                    vo.setJoinType(member.getJoinType());
                    voMap.put(user.getUserId(), vo);
                }
            }
            log.debug("获取群成员成功 groupId={} 耗时:{}ms 命中率:{}", groupId, System.currentTimeMillis() - startTime, cachedMembers != null ? "100%" : "DB");
            return Result.success(voMap);
        } catch (Exception e) {
            log.error("获取群成员异常 groupId={}", groupDto.getGroupId(), e);
            throw new GlobalException(ResultCode.FAIL, "获取群成员失败");
        }
    }

    /**
     * 退出群聊（Redisson RLock）
     */
    @Override
    public Result quitGroup(GroupDto groupDto) {
        long startTime = System.currentTimeMillis();
        String groupId = groupDto.getGroupId();
        String userId = groupDto.getUserId();
        RLock lock = redissonClient.getLock(GROUP_QUIT_LOCK_PREFIX + groupId + ":" + userId);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) { // 尝试获取锁，watchdog自动续租
                return Result.failed("退出操作过于频繁，请稍后重试");
            }

            ImGroupMemberPo member = imGroupFeign.getOneMember(groupId, userId);
            if (member == null) {
                return Result.failed("用户不在群聊中");
            }
            if (IMemberStatus.GROUP_OWNER.getCode().equals(member.getRole())) {
                throw new GlobalException(ResultCode.FAIL, "群主不可退出群聊");
            }

            boolean success = imGroupFeign.deleteById(member.getGroupMemberId());
            if (success) {
                // 异步失效缓存
                CompletableFuture.runAsync(() -> {
                    RMapCache<String, Object> cache = redissonClient.getMapCache(GROUP_MEMBERS_PREFIX);
                    cache.fastRemove(groupId); // 原子移除
                }, asyncTaskExecutor);
                log.info("退出群聊成功 groupId={} userId={}", groupId, userId);
            }
            log.debug("退出群聊耗时:{}ms", System.currentTimeMillis() - startTime);
            return success ? Result.success("退出群聊成功") : Result.failed("退出群聊失败");
        } catch (Exception e) {
            log.error("退出群聊异常 groupId={} userId={}", groupId, userId, e);
            throw new GlobalException(ResultCode.FAIL, "退出群聊失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock(); // 自动解锁
            }
        }
    }

    /**
     * 邀请成员（创建/邀请统一入口）
     */
    @Override
    public Result inviteGroup(GroupInviteDto dto) {
        Integer type = dto.getType();
        if (IMessageType.CREATE_GROUP.getCode().equals(type)) {
            return createGroup(dto);
        } else if (IMessageType.GROUP_INVITE.getCode().equals(type)) {
            return groupInvite(dto);
        }
        return Result.failed("无效邀请类型");
    }

    /**
     * 创建新群（批量插入，异步消息）
     */
    public Result<String> createGroup(@NonNull GroupInviteDto dto) {
        long startTime = System.currentTimeMillis();
        try {
            if (CollectionUtils.isEmpty(dto.getMemberIds())) {
                throw new GlobalException(ResultCode.FAIL, "至少需要一个被邀请人");
            }

            String groupId = imIdGeneratorFeign.getId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_message_id, String.class);

            // # TODO: 群名称
            String groupName = "默认群聊" + IdUtils.randomUUID();
            long now = DateTimeUtil.getCurrentUTCTimestamp();

            String ownerId = dto.getUserId();
            List<String> memberIds = dto.getMemberIds();

            // 批量构建成员
            List<ImGroupMemberPo> members = new ArrayList<>(memberIds.size() + 1);
            members.add(buildMember(groupId, ownerId, IMemberStatus.GROUP_OWNER, now));
            memberIds.forEach(id -> members.add(buildMember(groupId, id, IMemberStatus.NORMAL, now)));

            // 批量插入成员
            boolean membersOk = imGroupFeign.groupMessageMemberBatchInsert(members);
            if (!membersOk) {
                throw new GlobalException(ResultCode.FAIL, "群成员插入失败");
            }

            // 插入群信息
            ImGroupPo group = new ImGroupPo()
                    .setGroupId(groupId).setOwnerId(ownerId).setGroupType(1).setGroupName(groupName)
                    .setApplyJoinType(ImGroupJoinStatus.FREE.getCode())
                    .setStatus(IMStatus.YES.getCode()).setCreateTime(now).setDelFlag(IMStatus.YES.getCode());
            // 异步生成头像
            generateGroupAvatarAsync(groupId);

            boolean groupOk = imGroupFeign.insert(group);

            if (!groupOk) {
                throw new GlobalException(ResultCode.FAIL, "群信息插入失败");
            }

            // 异步发送系统消息
            CompletableFuture.runAsync(() -> messageService.sendGroupMessage(systemMessage(groupId, "已加入群聊,请尽情聊天吧")), asyncTaskExecutor);

            // Redisson缓存群信息（原子put）
            RMapCache<String, Object> groupCache = redissonClient.getMapCache(GROUP_CACHE_PREFIX);
            groupCache.fastPut(groupId, group, CACHE_TTL_SECONDS, TimeUnit.SECONDS);

            log.info("新建群聊成功 groupId={} owner={} members={}", groupId, ownerId, memberIds.size());
            log.debug("创建群聊耗时:{}ms", System.currentTimeMillis() - startTime);
            return Result.success(groupId);
        } catch (Exception e) {
            log.error("创建群聊异常 ownerId={}", dto.getUserId(), e);
            throw new GlobalException(ResultCode.FAIL, "创建群聊失败");
        }
    }

    /**
     * 群聊邀请（批量，Redisson RLock）
     */
    public Result groupInvite(@NonNull GroupInviteDto dto) {
        long startTime = System.currentTimeMillis();
        try {
            String groupId = StringUtils.hasText(dto.getGroupId()) ? dto.getGroupId() : imIdGeneratorFeign.getId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_message_id, String.class);
            String inviterId = dto.getUserId();
            List<String> inviteeIds = Optional.ofNullable(dto.getMemberIds()).orElse(Collections.emptyList());

            RLock lock = redissonClient.getLock(GROUP_INVITE_LOCK_PREFIX + groupId + ":" + inviterId);
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.failed("邀请操作过于频繁，请稍后重试");
            }

            // Redisson缓存或查询现有成员
            RMapCache<String, Object> memberCache = redissonClient.getMapCache(GROUP_MEMBERS_PREFIX);
            Object cachedMembers = memberCache.get(groupId);
            List<ImGroupMemberPo> existingMembers;
            if (cachedMembers != null) {
                existingMembers = (List<ImGroupMemberPo>) cachedMembers;
            } else {
                existingMembers = imGroupFeign.getGroupMemberList(groupId);
                memberCache.fastPut(groupId, existingMembers, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }

            Set<String> existingIds = existingMembers.stream().map(ImGroupMemberPo::getMemberId).collect(Collectors.toSet());
            if (!existingIds.contains(inviterId)) {
                throw new GlobalException(ResultCode.FAIL, "用户不在该群组中，不可邀请新成员");
            }

            List<String> newInvitees = inviteeIds.stream().filter(id -> !existingIds.contains(id)).distinct().collect(Collectors.toList());
            if (newInvitees.isEmpty()) {
                return Result.success(groupId);
            }

            // 批量构建邀请请求
            long now = DateTimeUtil.getCurrentUTCTimestamp();
            long expireTime = now + 7L * 24 * 3600;
            RMapCache<String, Object> groupCache = redissonClient.getMapCache(GROUP_CACHE_PREFIX);
            Object cachedGroup = groupCache.get(groupId);
            ImGroupPo groupPo = cachedGroup != null ? (ImGroupPo) cachedGroup : imGroupFeign.getOneGroup(groupId);
            if (groupPo != null && cachedGroup == null) {
                groupCache.fastPut(groupId, groupPo, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }
            String verifierId = groupPo != null ? groupPo.getOwnerId() : inviterId;

            List<ImGroupInviteRequestPo> requests = new ArrayList<>(newInvitees.size());
            for (String toId : newInvitees) {
                String requestId = imIdGeneratorFeign.getId(IdGeneratorConstant.uuid, IdGeneratorConstant.group_invite_id, String.class);
                ImGroupInviteRequestPo po = new ImGroupInviteRequestPo()
                        .setRequestId(requestId).setGroupId(groupId).setFromId(inviterId).setToId(toId)
                        .setVerifierId(verifierId).setVerifierStatus(0).setMessage(dto.getMessage())
                        .setApproveStatus(0).setAddSource(dto.getAddSource()).setExpireTime(expireTime)
                        .setCreateTime(now).setDelFlag(1);
                requests.add(po);
            }

            // 批量保存邀请请求
            boolean dbOk = imGroupFeign.groupInviteSaveOrUpdateBatch(requests);
            if (!dbOk) {
                throw new GlobalException(ResultCode.FAIL, "保存邀请请求失败");
            }

            // 异步批量发送邀请消息
            CompletableFuture.runAsync(() -> sendBatchInviteMessages(groupId, inviterId, newInvitees, groupPo), asyncTaskExecutor);

            log.info("群邀请成功 groupId={} inviter={} newMembers={}", groupId, inviterId, newInvitees.size());
            log.debug("群邀请耗时:{}ms", System.currentTimeMillis() - startTime);
            return Result.success(groupId);
        } catch (Exception e) {
            log.error("群邀请异常 groupId={} inviterId={}", dto.getGroupId(), dto.getUserId(), e);
            throw new GlobalException(ResultCode.FAIL, "群邀请失败");
        } finally {
            RLock lock = redissonClient.getLock(GROUP_INVITE_LOCK_PREFIX + dto.getGroupId() + ":" + dto.getUserId());
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 批准群邀请（Redisson RLock + 缓存失效）
     */
    @Override
    public Result approveGroupInvite(GroupInviteDto dto) {
        long startTime = System.currentTimeMillis();
        try {
            String groupId = dto.getGroupId();
            String userId = dto.getUserId();
            String inviterId = dto.getInviterId();
            Integer approveStatus = dto.getApproveStatus();

            if (!StringUtils.hasText(groupId) || !StringUtils.hasText(userId) || approveStatus == null) {
                return Result.success("信息不完整");
            }

            if (approveStatus.equals(0)) return Result.success("待处理群聊邀请");
            if (approveStatus.equals(2)) return Result.success("已拒绝群聊邀请");

            // Redisson缓存群信息
            RMapCache<String, Object> groupCache = redissonClient.getMapCache(GROUP_CACHE_PREFIX);
            Object cachedGroup = groupCache.get(groupId);
            ImGroupPo groupPo = cachedGroup != null ? (ImGroupPo) cachedGroup : imGroupFeign.getOneGroup(groupId);
            if (groupPo == null) return Result.success("群不存在");
            if (cachedGroup == null) {
                groupCache.fastPut(groupId, groupPo, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }

            // 检查加入类型
            if (ImGroupJoinStatus.BAN.getCode().equals(groupPo.getApplyJoinType())) {
                return Result.success("群不允许加入");
            }

            if (ImGroupJoinStatus.APPROVE.getCode().equals(groupPo.getApplyJoinType())) {
                // 异步发送审批请求
                CompletableFuture.runAsync(() -> sendJoinApprovalRequestToAdmins(groupId, inviterId, userId, groupPo), asyncTaskExecutor);
                return Result.success("已发送入群验证请求，等待审核");
            }

            // 直接加入（Redisson锁防重复）
            RLock lock = redissonClient.getLock(GROUP_JOIN_LOCK_PREFIX + groupId + ":" + userId);
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.failed("加入操作过于频繁，请稍后重试");
            }

            ImGroupMemberPo member = imGroupFeign.getOneMember(groupId, userId);
            if (member != null && IMemberStatus.NORMAL.getCode().equals(member.getRole())) {
                return Result.failed("用户已加入群聊");
            }

            long now = DateTimeUtil.getCurrentUTCTimestamp();
            ImGroupMemberPo newMember = buildMember(groupId, userId, IMemberStatus.NORMAL, now);
            boolean success = imGroupFeign.groupMessageMemberBatchInsert(List.of(newMember));
            if (success) {
                // 异步更新缓存/头像/通知
                CompletableFuture.runAsync(() -> {
                    RMapCache<String, Object> memberCache = redissonClient.getMapCache(GROUP_MEMBERS_PREFIX);
                    memberCache.fastRemove(groupId); // 原子失效
                    List<ImGroupMemberPo> updatedMembers = imGroupFeign.getGroupMemberList(groupId);
                    memberCache.fastPut(groupId, updatedMembers, CACHE_TTL_SECONDS, TimeUnit.SECONDS); // 重新缓存
                    if (updatedMembers.size() < 10) {
                        ImGroupPo update = new ImGroupPo().setGroupId(groupId);
                        imGroupFeign.updateById(update);
                        // 异步生成头像
                        generateGroupAvatarAsync(groupId);
                    }
                    sendJoinNotification(groupId, inviterId, userId);
                }, asyncTaskExecutor);
            }

            log.info("批准邀请成功 groupId={} userId={}", groupId, userId);
            log.debug("批准邀请耗时:{}ms", System.currentTimeMillis() - startTime);
            return success ? Result.success("成功加入群聊") : Result.failed("加入群聊失败");
        } catch (Exception e) {
            log.error("批准邀请异常 groupId={} userId={}", dto.getGroupId(), dto.getUserId(), e);
            throw new GlobalException(ResultCode.FAIL, "批准邀请失败");
        } finally {
            RLock lock = redissonClient.getLock(GROUP_JOIN_LOCK_PREFIX + dto.getGroupId() + ":" + dto.getUserId());
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取群信息（Redisson RMapCache）
     */
    @Override
    public Result<?> groupInfo(@NonNull GroupDto groupDto) {
        try {
            RMapCache<String, Object> cache = redissonClient.getMapCache(GROUP_CACHE_PREFIX);
            Object cached = cache.get(groupDto.getGroupId());
            ImGroupPo group = cached != null ? (ImGroupPo) cached : imGroupFeign.getOneGroup(groupDto.getGroupId());
            if (group != null && cached == null) {
                cache.fastPut(groupDto.getGroupId(), group, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }
            return Result.success(group != null ? group : new ImGroupPo()); // 兜底
        } catch (Exception e) {
            log.error("获取群信息异常 groupId={}", groupDto.getGroupId(), e);
            throw new GlobalException(ResultCode.FAIL, "获取群信息失败");
        }
    }

    // 私有方法：构建成员（不变）
    private ImGroupMemberPo buildMember(String groupId, String memberId, IMemberStatus role, long joinTime) {
        return new ImGroupMemberPo()
                .setGroupId(groupId).setGroupMemberId(IdUtils.snowflakeIdStr()).setMemberId(memberId)
                .setRole(role.getCode()).setMute(IMStatus.NO.getCode()).setDelFlag(IMStatus.YES.getCode())
                .setJoinTime(joinTime);
    }

    // 私有方法：系统消息（不变）
    private IMGroupMessage systemMessage(String groupId, String message) {
        return IMGroupMessage.builder().groupId(groupId).fromId(IMConstant.SYSTEM)
                .messageContentType(IMessageContentType.TIP.getCode())
                .messageBody(new IMessage.TextMessageBody().setText(message)).build();
    }

    // 异步生成群头像（不变）
    public void generateGroupAvatarAsync(String groupId) {
        try {
            List<String> avatars = imGroupFeign.getNinePeopleAvatar(groupId);
            File headFile = groupHeadImageUtil.getCombinationOfhead(avatars, "defaultGroupHead" + groupId);
            MultipartFile mpFile = fileService.fileToImageMultipartFile(headFile);
            String avatarUrl = fileService.uploadFile(mpFile).getPath();
            ImGroupPo update = new ImGroupPo().setGroupId(groupId).setAvatar(avatarUrl);
            imGroupFeign.updateById(update);
            // 更新Redisson缓存
            RMapCache<String, Object> cache = redissonClient.getMapCache(GROUP_CACHE_PREFIX);
            ImGroupPo cachedGroup = (ImGroupPo) cache.get(groupId);
            if (cachedGroup != null) {
                cachedGroup.setAvatar(avatarUrl);
                cache.fastPut(groupId, cachedGroup, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("异步生成群头像失败 groupId={}", groupId, e);
        }
    }

    // 异步批量发送邀请消息（不变，但用Redisson缓存groupPo）
    @Async
    public void sendBatchInviteMessages(String groupId, String inviterId, List<String> invitees, ImGroupPo groupPo) {
        try {
            ImUserDataPo inviterInfo = imUserFeign.getOne(inviterId);
            List<IMSingleMessage> messages = new ArrayList<>(invitees.size());
            for (String inviteeId : invitees) {
                IMSingleMessage msg = IMSingleMessage.builder()
                        .messageTempId(IdUtils.snowflakeIdStr()).fromId(inviterId).toId(inviteeId)
                        .messageContentType(IMessageContentType.GROUP_INVITE.getCode())
                        .messageTime(DateTimeUtil.getCurrentUTCTimestamp()).messageType(IMessageType.SINGLE_MESSAGE.getCode())
                        .build();
                IMessage.MessageBody body = new IMessage.GroupInviteMessageBody()
                        .setRequestId("") // 从DB取或预生成
                        .setGroupId(groupId).setUserId(inviteeId).setGroupAvatar(groupPo.getAvatar())
                        .setGroupName(groupPo.getGroupName()).setInviterId(inviterId)
                        .setInviterName(inviterInfo.getName()).setApproveStatus(0);
                msg.setMessageBody(body);
                messages.add(msg);
            }
            // 批量发送
            CompletableFuture.allOf(messages.stream().map(msg -> CompletableFuture.runAsync(() -> messageService.sendSingleMessage(msg), asyncTaskExecutor)).toArray(CompletableFuture[]::new)).get();
        } catch (Exception e) {
            log.error("批量发送邀请消息失败 groupId={} invitees={}", groupId, invitees.size(), e);
        }
    }

    // 异步发送加入通知（不变）
    @Async
    public void sendJoinNotification(String groupId, String inviterId, String userId) {
        try {
            ImUserDataPo invitee = imUserFeign.getOne(userId);
            ImUserDataPo inviter = imUserFeign.getOne(inviterId);
            String msg = "\"" + (inviter != null ? inviter.getName() : inviterId) + "\" 邀请 \"" +
                    (invitee != null ? invitee.getName() : userId) + "\" 加入群聊";
            messageService.sendGroupMessage(systemMessage(groupId, msg));
        } catch (Exception e) {
            log.error("发送加入通知失败 groupId={} userId={}", groupId, userId, e);
        }
    }

    // 向管理员发送审批请求（批量，Redisson缓存）
    private void sendJoinApprovalRequestToAdmins(String groupId, String inviterId, String inviteeId, ImGroupPo groupPo) {
        RMapCache<String, Object> memberCache = redissonClient.getMapCache(GROUP_MEMBERS_PREFIX);
        Object cachedMembers = memberCache.get(groupId);
        List<ImGroupMemberPo> members = cachedMembers != null ? (List<ImGroupMemberPo>) cachedMembers : imGroupFeign.getGroupMemberList(groupId);
        if (CollectionUtils.isEmpty(members)) return;

        List<String> adminIds = members.stream()
                .filter(m -> IMemberStatus.GROUP_OWNER.getCode().equals(m.getRole()) || IMemberStatus.ADMIN.getCode().equals(m.getRole()))
                .map(ImGroupMemberPo::getMemberId).distinct().collect(Collectors.toList());
        if (adminIds.isEmpty() && StringUtils.hasText(groupPo.getOwnerId())) {
            adminIds = List.of(groupPo.getOwnerId());
        }

        ImUserDataPo inviterInfo = imUserFeign.getOne(inviterId);
        ImUserDataPo inviteeInfo = imUserFeign.getOne(inviteeId);

        List<IMSingleMessage> msgs = new ArrayList<>(adminIds.size());
        for (String adminId : adminIds) {
            IMSingleMessage msg = IMSingleMessage.builder()
                    .messageTempId(IdUtils.snowflakeIdStr()).fromId(inviterId).toId(adminId)
                    .messageContentType(IMessageContentType.GROUP_JOIN_APPROVE.getCode())
                    .messageTime(DateTimeUtil.getCurrentUTCTimestamp()).build();
            IMessage.MessageBody body = new IMessage.GroupInviteMessageBody()
                    .setInviterId(inviterId).setGroupId(groupId).setUserId(inviteeId)
                    .setGroupAvatar(groupPo.getAvatar()).setGroupName(groupPo.getGroupName())
                    .setInviterName(inviterInfo.getName()).setApproveStatus(0);
            msg.setMessageBody(body);
            msgs.add(msg);
        }
        // 批量异步发送
        CompletableFuture.allOf(msgs.stream().map(m -> CompletableFuture.runAsync(() -> messageService.sendSingleMessage(m), asyncTaskExecutor)).toArray(CompletableFuture[]::new)).join();
    }
}