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
import com.xy.lucky.domain.vo.GroupMemberVo;
import com.xy.lucky.dubbo.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.api.database.group.ImGroupInviteRequestDubboService;
import com.xy.lucky.dubbo.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.api.id.ImIdDubboService;
import com.xy.lucky.general.response.domain.Result;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.server.api.IdGeneratorConstant;
import com.xy.lucky.server.exception.GlobalException;
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

    /**
     * 获取群成员信息（移除Redisson RMapCache缓存）
     */
    @Override
    public Result<?> getGroupMembers(GroupDto groupDto) {
        long startTime = System.currentTimeMillis();
        try {
            String groupId = groupDto.getGroupId();

            // 直接从数据库获取群成员信息，不再使用缓存
            List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(groupId);

            // 如果没有成员，返回空映射
            if (CollectionUtils.isEmpty(members)) {
                return Result.success(Collections.emptyMap());
            }

            // 批量查询用户并构建VO
            List<String> memberIds = members.stream()
                    .map(ImGroupMemberPo::getMemberId)
                    .collect(Collectors.toList());

            List<ImUserDataPo> users = imUserDataDubboService.selectByIds(memberIds);
            Map<String, ImUserDataPo> userMap = users.stream()
                    .collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));

            Map<String, GroupMemberVo> voMap = new HashMap<>(members.size());
            for (ImGroupMemberPo member : members) {
                ImUserDataPo user = userMap.get(member.getMemberId());
                if (user != null) {
                    GroupMemberVo vo = GroupMemberBeanMapper.INSTANCE.toGroupMemberVo(member);
                    vo.setRole(member.getRole());
                    vo.setMute(member.getMute());
                    vo.setAlias(member.getAlias());
                    vo.setJoinType(member.getJoinType());
                    voMap.put(user.getUserId(), vo);
                }
            }

            log.debug("获取群成员成功 groupId={} 耗时:{}ms",
                    groupId, System.currentTimeMillis() - startTime);
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

        RLock lock = redissonClient.getLock(LockConstants.QUIT_PREFIX + groupId + ":" + userId);
        try {
            // 尝试获取锁，超时后自动释放
            if (!lock.tryLock(LockConstants.WAIT_TIME, LockConstants.LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.failed("退出操作过于频繁，请稍后重试");
            }

            // 检查用户是否在群聊中
            ImGroupMemberPo member = imGroupMemberDubboService.selectOne(groupId, userId);
            if (member == null) {
                return Result.failed("用户不在群聊中");
            }

            // 群主不能退出群聊
            if (IMemberStatus.GROUP_OWNER.getCode().equals(member.getRole())) {
                throw new GlobalException(ResultCode.FAIL, "群主不可退出群聊");
            }

            // 删除群成员
            boolean success = imGroupMemberDubboService.deleteById(member.getGroupMemberId());
            if (success) {
                log.info("退出群聊成功 groupId={} userId={}", groupId, userId);
            }

            log.debug("退出群聊耗时:{}ms", System.currentTimeMillis() - startTime);
            return success ? Result.success("退出群聊成功") : Result.failed("退出群聊失败");
        } catch (Exception e) {
            log.error("退出群聊异常 groupId={} userId={}", groupId, userId, e);
            throw new GlobalException(ResultCode.FAIL, "退出群聊失败");
        } finally {
            // 确保当前线程持有锁后再释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

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

    @Resource
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    /**
     * 创建新群（批量插入，异步消息）
     */
    public Result<String> createGroup(@NonNull GroupInviteDto dto) {
        long startTime = System.currentTimeMillis();
        try {
            // 检查邀请成员列表
            if (CollectionUtils.isEmpty(dto.getMemberIds())) {
                throw new GlobalException(ResultCode.FAIL, "至少需要一个被邀请人");
            }

            // 生成群ID
            String groupId = imIdDubboService.generateId(
                    IdGeneratorConstant.uuid,
                    IdGeneratorConstant.group_message_id).getStringId();

            // 生成群名称
            String groupName = "默认群聊" + IdUtils.randomUUID();
            long now = DateTimeUtils.getCurrentUTCTimestamp();

            String ownerId = dto.getUserId();
            List<String> memberIds = dto.getMemberIds();

            // 批量构建成员
            List<ImGroupMemberPo> members = new ArrayList<>(memberIds.size() + 1);
            members.add(buildMember(groupId, ownerId, IMemberStatus.GROUP_OWNER, now));
            memberIds.forEach(id -> members.add(buildMember(groupId, id, IMemberStatus.NORMAL, now)));

            // 批量插入成员
            boolean membersOk = imGroupMemberDubboService.batchInsert(members);
            if (!membersOk) {
                throw new GlobalException(ResultCode.FAIL, "群成员插入失败");
            }

            // 插入群信息
            ImGroupPo group = new ImGroupPo()
                    .setGroupId(groupId)
                    .setOwnerId(ownerId)
                    .setGroupType(1)
                    .setGroupName(groupName)
                    .setApplyJoinType(ImGroupJoinStatus.FREE.getCode())
                    .setStatus(IMStatus.YES.getCode())
                    .setCreateTime(now)
                    .setDelFlag(IMStatus.YES.getCode());

            // 异步生成头像
            generateGroupAvatarAsync(groupId);

            boolean groupOk = imGroupDubboService.insert(group);
            if (!groupOk) {
                throw new GlobalException(ResultCode.FAIL, "群信息插入失败");
            }

            // 异步发送系统消息
            CompletableFuture.runAsync(
                    () -> messageService.sendGroupMessage(systemMessage(groupId, "已加入群聊,请尽情聊天吧")),
                    asyncTaskExecutor);

            // Redisson缓存群信息
            RMapCache<String, Object> groupCache = redissonClient.getMapCache(CacheConstants.GROUP_INFO_PREFIX);
            groupCache.fastPut(groupId, group, CacheConstants.TTL_SECONDS, TimeUnit.SECONDS);

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
            String groupId = StringUtils.hasText(dto.getGroupId()) ?
                    dto.getGroupId() :
                    imIdDubboService.generateId(
                            IdGeneratorConstant.uuid,
                            IdGeneratorConstant.group_message_id).getStringId();

            String inviterId = dto.getUserId();
            List<String> inviteeIds = Optional.ofNullable(dto.getMemberIds())
                    .orElse(Collections.emptyList());

            // 获取邀请锁
            RLock lock = redissonClient.getLock(LockConstants.INVITE_PREFIX + groupId + ":" + inviterId);
            if (!lock.tryLock(LockConstants.WAIT_TIME, LockConstants.LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.failed("邀请操作过于频繁，请稍后重试");
            }

            try {
                // 直接从数据库获取现有成员，不再使用缓存
                List<ImGroupMemberPo> existingMembers = imGroupMemberDubboService.selectList(groupId);

                // 检查邀请人是否在群中
                Set<String> existingIds = existingMembers.stream()
                        .map(ImGroupMemberPo::getMemberId)
                        .collect(Collectors.toSet());

                if (!existingIds.contains(inviterId)) {
                    throw new GlobalException(ResultCode.FAIL, "用户不在该群组中，不可邀请新成员");
                }

                // 过滤出新邀请的成员
                List<String> newInvitees = inviteeIds.stream()
                        .filter(id -> !existingIds.contains(id))
                        .distinct()
                        .collect(Collectors.toList());

                if (newInvitees.isEmpty()) {
                    return Result.success(groupId);
                }

                // 获取群信息
                long now = DateTimeUtils.getCurrentUTCTimestamp();
                long expireTime = now + 7L * 24 * 3600;

                RMapCache<String, Object> groupCache = redissonClient.getMapCache(CacheConstants.GROUP_INFO_PREFIX);
                Object cachedGroup = groupCache.get(groupId);
                ImGroupPo groupPo = cachedGroup != null ?
                        (ImGroupPo) cachedGroup :
                        imGroupDubboService.selectOne(groupId);

                if (groupPo != null && cachedGroup == null) {
                    groupCache.fastPut(groupId, groupPo, CacheConstants.TTL_SECONDS, TimeUnit.SECONDS);
                }

                String verifierId = groupPo != null ? groupPo.getOwnerId() : inviterId;

                // 批量构建邀请请求
                List<ImGroupInviteRequestPo> requests = new ArrayList<>(newInvitees.size());
                for (String toId : newInvitees) {
                    String requestId = imIdDubboService.generateId(
                            IdGeneratorConstant.uuid,
                            IdGeneratorConstant.group_invite_id).getStringId();

                    ImGroupInviteRequestPo po = new ImGroupInviteRequestPo()
                            .setRequestId(requestId)
                            .setGroupId(groupId)
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

                // 批量保存邀请请求
                Boolean dbOk = imGroupInviteRequestDubboService.batchInsert(requests);
                if (!dbOk) {
                    throw new GlobalException(ResultCode.FAIL, "保存邀请请求失败");
                }

                // 异步批量发送邀请消息
                CompletableFuture.runAsync(
                        () -> sendBatchInviteMessages(groupId, inviterId, newInvitees, groupPo),
                        asyncTaskExecutor);

                log.info("群邀请成功 groupId={} inviter={} newMembers={}", groupId, inviterId, newInvitees.size());
                log.debug("群邀请耗时:{}ms", System.currentTimeMillis() - startTime);
                return Result.success(groupId);
            } catch (Exception e) {
                log.error("群邀请异常 groupId={} inviterId={}", dto.getGroupId(), dto.getUserId(), e);
                throw new GlobalException(ResultCode.FAIL, "群邀请失败");
            }
        } catch (Exception e) {
            log.error("群邀请异常 groupId={} inviterId={}", dto.getGroupId(), dto.getUserId(), e);
            throw new GlobalException(ResultCode.FAIL, "群邀请失败");
        } finally {
            RLock lock = redissonClient.getLock(LockConstants.INVITE_PREFIX + dto.getGroupId() + ":" + dto.getUserId());
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
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

            // 参数校验
            if (!StringUtils.hasText(groupId) || !StringUtils.hasText(userId) || approveStatus == null) {
                return Result.success("信息不完整");
            }

            // 待处理状态
            if (approveStatus.equals(0)) {
                return Result.success("待处理群聊邀请");
            }

            // 已拒绝状态
            if (approveStatus.equals(2)) {
                return Result.success("已拒绝群聊邀请");
            }

            // 获取群信息
            RMapCache<String, Object> groupCache = redissonClient.getMapCache(CacheConstants.GROUP_INFO_PREFIX);
            Object cachedGroup = groupCache.get(groupId);
            ImGroupPo groupPo = cachedGroup != null ?
                    (ImGroupPo) cachedGroup :
                    imGroupDubboService.selectOne(groupId);

            if (groupPo == null) {
                return Result.success("群不存在");
            }

            if (cachedGroup == null) {
                groupCache.fastPut(groupId, groupPo, CacheConstants.TTL_SECONDS, TimeUnit.SECONDS);
            }

            // 检查加入类型
            if (ImGroupJoinStatus.BAN.getCode().equals(groupPo.getApplyJoinType())) {
                return Result.success("群不允许加入");
            }

            if (ImGroupJoinStatus.APPROVE.getCode().equals(groupPo.getApplyJoinType())) {
                // 异步发送审批请求
                CompletableFuture.runAsync(
                        () -> sendJoinApprovalRequestToAdmins(groupId, inviterId, userId, groupPo),
                        asyncTaskExecutor);
                return Result.success("已发送入群验证请求，等待审核");
            }

            // 直接加入逻辑
            return processDirectJoin(groupId, userId, inviterId, startTime);
        } catch (Exception e) {
            log.error("批准邀请异常 groupId={} userId={}", dto.getGroupId(), dto.getUserId(), e);
            throw new GlobalException(ResultCode.FAIL, "批准邀请失败");
        }
    }

    /**
     * 处理直接加入群聊逻辑
     *
     * @param groupId   群ID
     * @param userId    用户ID
     * @param inviterId 邀请人ID
     * @param startTime 开始时间
     * @return 处理结果
     */
    private Result processDirectJoin(String groupId, String userId, String inviterId, long startTime) {
        // 获取加入锁
        RLock lock = redissonClient.getLock(LockConstants.JOIN_PREFIX + groupId + ":" + userId);
        try {
            // 尝试获取锁
            if (!lock.tryLock(LockConstants.WAIT_TIME, LockConstants.LEASE_TIME, TimeUnit.SECONDS)) {
                return Result.failed("加入操作过于频繁，请稍后重试");
            }

            // 检查用户是否已加入
            ImGroupMemberPo member = imGroupMemberDubboService.selectOne(groupId, userId);
            if (member != null && IMemberStatus.NORMAL.getCode().equals(member.getRole())) {
                return Result.failed("用户已加入群聊");
            }

            // 添加新成员
            long now = DateTimeUtils.getCurrentUTCTimestamp();
            ImGroupMemberPo newMember = buildMember(groupId, userId, IMemberStatus.NORMAL, now);
            boolean success = imGroupMemberDubboService.batchInsert(List.of(newMember));

            if (success) {
                // 异步更新群信息和发送通知
                CompletableFuture.runAsync(() -> updateGroupInfoAndNotify(groupId, inviterId, userId), asyncTaskExecutor);
            }

            log.info("批准邀请成功 groupId={} userId={}", groupId, userId);
            log.debug("批准邀请耗时:{}ms", System.currentTimeMillis() - startTime);
            return success ? Result.success("成功加入群聊") : Result.failed("加入群聊失败");
        } catch (Exception e) {
            log.error("处理直接加入异常 groupId={} userId={}", groupId, userId, e);
            throw new GlobalException(ResultCode.FAIL, "处理加入请求失败");
        } finally {
            // 确保当前线程持有锁后再释放
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 更新群信息并发送通知
     *
     * @param groupId   群ID
     * @param inviterId 邀请人ID
     * @param userId    用户ID
     */
    private void updateGroupInfoAndNotify(String groupId, String inviterId, String userId) {
        try {
            // 获取最新的成员列表
            List<ImGroupMemberPo> updatedMembers = imGroupMemberDubboService.selectList(groupId);

            // 如果成员少于10人，更新群信息并重新生成头像
            if (updatedMembers.size() < 10) {
                ImGroupPo update = new ImGroupPo().setGroupId(groupId);
                imGroupDubboService.update(update);
                // 异步生成头像
                generateGroupAvatarAsync(groupId);
            }

            // 发送加入通知
            sendJoinNotification(groupId, inviterId, userId);
        } catch (Exception e) {
            log.error("更新群信息或发送通知失败 groupId={} userId={}", groupId, userId, e);
        }
    }

    /**
     * 获取群信息（Redisson RMapCache）
     */
    @Override
    public Result<?> groupInfo(@NonNull GroupDto groupDto) {
        try {
            RMapCache<String, Object> cache = redissonClient.getMapCache(CacheConstants.GROUP_INFO_PREFIX);
            Object cached = cache.get(groupDto.getGroupId());

            ImGroupPo group;
            if (cached != null) {
                group = (ImGroupPo) cached;
            } else {
                group = imGroupDubboService.selectOne(groupDto.getGroupId());
                if (group != null) {
                    cache.fastPut(groupDto.getGroupId(), group, CacheConstants.TTL_SECONDS, TimeUnit.SECONDS);
                }
            }

            // 返回群信息，如果不存在则返回空对象
            return Result.success(group != null ? group : new ImGroupPo());
        } catch (Exception e) {
            log.error("获取群信息异常 groupId={}", groupDto.getGroupId(), e);
            throw new GlobalException(ResultCode.FAIL, "获取群信息失败");
        }
    }

    /**
     * 更新群信息
     */
    @Override
    public Result updateGroupInfo(GroupDto groupDto) {
        long startTime = System.currentTimeMillis();
        try {
            String groupId = groupDto.getGroupId();
            
            // 检查群组是否存在
            ImGroupPo existingGroup = imGroupDubboService.selectOne(groupId);
            if (existingGroup == null) {
                return Result.failed("群组不存在");
            }
            
            // 构建更新对象
            ImGroupPo updateGroup = new ImGroupPo().setGroupId(groupId);
            
            // 更新群名称
            if (StringUtils.hasText(groupDto.getGroupName())) {
                updateGroup.setGroupName(groupDto.getGroupName());
            }
            
            // 更新群头像
            if (StringUtils.hasText(groupDto.getAvatar())) {
                updateGroup.setAvatar(groupDto.getAvatar());
            }
            
            // 更新群简介
            if (StringUtils.hasText(groupDto.getIntroduction())) {
                updateGroup.setIntroduction(groupDto.getIntroduction());
            }
            
            // 更新群公告
            if (StringUtils.hasText(groupDto.getNotification())) {
                updateGroup.setNotification(groupDto.getNotification());
            }
            
            // 更新数据库
            boolean success = imGroupDubboService.update(updateGroup);

            log.debug("更新群组信息耗时:{}ms", System.currentTimeMillis() - startTime);
            return success ? Result.success("更新成功") : Result.failed("更新失败");
        } catch (Exception e) {
            log.error("更新群组信息异常 groupId={}", groupDto.getGroupId(), e);
            throw new GlobalException(ResultCode.FAIL, "更新群组信息失败");
        }
    }

    // 私有方法：构建成员
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

    // 私有方法：系统消息
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

    // 异步生成群头像
    public void generateGroupAvatarAsync(String groupId) {
        try {
            List<String> avatars = imGroupMemberDubboService.selectNinePeopleAvatar(groupId);
            File headFile = groupHeadImageUtils.getCombinationOfhead(avatars, "defaultGroupHead" + groupId);
            MultipartFile mpFile = fileService.fileToImageMultipartFile(headFile);
            String avatarUrl = fileService.uploadFile(mpFile).getPath();

            ImGroupPo update = new ImGroupPo().setGroupId(groupId).setAvatar(avatarUrl);
            imGroupDubboService.update(update);

            // 更新Redisson缓存
            RMapCache<String, Object> cache = redissonClient.getMapCache(CacheConstants.GROUP_INFO_PREFIX);
            ImGroupPo cachedGroup = (ImGroupPo) cache.get(groupId);
            if (cachedGroup != null) {
                cachedGroup.setAvatar(avatarUrl);
                cache.fastPut(groupId, cachedGroup, CacheConstants.TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("异步生成群头像失败 groupId={}", groupId, e);
        }
    }

    // 异步批量发送邀请消息（使用Redisson缓存groupPo）
    @Async
    public void sendBatchInviteMessages(String groupId, String inviterId, List<String> invitees, ImGroupPo groupPo) {
        try {
            ImUserDataPo inviterInfo = imUserDataDubboService.selectOne(inviterId);
            List<IMSingleMessage> messages = new ArrayList<>(invitees.size());

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
                        .setRequestId("") // 从DB取或预生成
                        .setGroupId(groupId)
                        .setUserId(inviteeId)
                        .setGroupAvatar(groupPo.getAvatar())
                        .setGroupName(groupPo.getGroupName())
                        .setInviterId(inviterId)
                        .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                        .setApproveStatus(0);
                msg.setMessageBody(body);
                messages.add(msg);
            }

            // 批量发送
            CompletableFuture.allOf(
                    messages.stream()
                            .map(msg -> CompletableFuture.runAsync(
                                    () -> messageService.sendSingleMessage(msg),
                            asyncTaskExecutor))
                    .toArray(CompletableFuture[]::new)
            ).get();
        } catch (Exception e) {
            log.error("批量发送邀请消息失败 groupId={} invitees={}", groupId, invitees.size(), e);
        }
    }

    // 异步发送加入通知
    @Async
    public void sendJoinNotification(String groupId, String inviterId, String userId) {
        try {
            ImUserDataPo invitee = imUserDataDubboService.selectOne(userId);
            ImUserDataPo inviter = imUserDataDubboService.selectOne(inviterId);

            String msg = "\"" + (inviter != null ? inviter.getName() : inviterId) + "\" 邀请 \"" +
                    (invitee != null ? invitee.getName() : userId) + "\" 加入群聊";

            messageService.sendGroupMessage(systemMessage(groupId, msg));
        } catch (Exception e) {
            log.error("发送加入通知失败 groupId={} userId={}", groupId, userId, e);
        }
    }

    // 向管理员发送审批请求（批量，使用Redisson缓存）
    private void sendJoinApprovalRequestToAdmins(String groupId, String inviterId, String inviteeId, ImGroupPo groupPo) {
        try {
            // 直接从数据库获取成员列表，不再使用缓存
            List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(groupId);

            if (CollectionUtils.isEmpty(members)) {
                return;
            }

            // 获取管理员和群主ID
            List<String> adminIds = members.stream()
                    .filter(m -> IMemberStatus.GROUP_OWNER.getCode().equals(m.getRole()) ||
                            IMemberStatus.ADMIN.getCode().equals(m.getRole()))
                    .map(ImGroupMemberPo::getMemberId)
                    .distinct()
                    .collect(Collectors.toList());

            if (adminIds.isEmpty() && StringUtils.hasText(groupPo.getOwnerId())) {
                adminIds = List.of(groupPo.getOwnerId());
            }

            ImUserDataPo inviterInfo = imUserDataDubboService.selectOne(inviterId);
            ImUserDataPo inviteeInfo = imUserDataDubboService.selectOne(inviteeId);

            List<IMSingleMessage> msgs = new ArrayList<>(adminIds.size());
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
                        .setGroupAvatar(groupPo.getAvatar())
                        .setGroupName(groupPo.getGroupName())
                        .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                        .setApproveStatus(0);
                msg.setMessageBody(body);
                msgs.add(msg);
            }

            // 批量异步发送
            CompletableFuture.allOf(
                    msgs.stream()
                            .map(m -> CompletableFuture.runAsync(
                                    () -> messageService.sendSingleMessage(m),
                                    asyncTaskExecutor))
                            .toArray(CompletableFuture[]::new)
            ).join();
        } catch (Exception e) {
            log.error("向管理员发送审批请求失败 groupId={} inviterId={} inviteeId={}", groupId, inviterId, inviteeId, e);
        }
    }

    /**
     * 更新群成员信息（群昵称、备注等）
     */
    @Override
    public Result updateGroupMember(GroupMemberDto groupMemberDto) {
        long startTime = System.currentTimeMillis();
        try {
            String groupId = groupMemberDto.getGroupId();
            String userId = groupMemberDto.getUserId();

            // 查询群成员信息
            ImGroupMemberPo member = imGroupMemberDubboService.selectOne(groupId, userId);
            if (member == null) {
                return Result.failed("用户不在群聊中");
            }

            // 构建更新对象
            ImGroupMemberPo updateMember = new ImGroupMemberPo()
                    .setGroupMemberId(member.getGroupMemberId());

            // 更新群昵称（如果提供）
            if (StringUtils.hasText(groupMemberDto.getAlias())) {
                updateMember.setAlias(groupMemberDto.getAlias());
            }

            // 更新备注（如果提供）
            if (StringUtils.hasText(groupMemberDto.getRemark())) {
                updateMember.setRemark(groupMemberDto.getRemark());
            }

            // 更新数据库
            boolean success = imGroupMemberDubboService.update(updateMember);
            if (success) {
                log.info("更新群成员信息成功 groupId={} userId={}", groupId, userId);
            }

            log.debug("更新群成员信息耗时:{}ms", System.currentTimeMillis() - startTime);
            return success ? Result.success("更新成功") : Result.failed("更新失败");
        } catch (Exception e) {
            log.error("更新群成员信息异常 groupId={} userId={}",
                    groupMemberDto.getGroupId(), groupMemberDto.getUserId(), e);
            throw new GlobalException(ResultCode.FAIL, "更新群成员信息失败");
        }
    }

    /**
     * Redis缓存相关常量
     */
    private static final class CacheConstants {
        /**
         * 群信息缓存前缀
         */
        static final String GROUP_INFO_PREFIX = "group:info:";
        /**
         * 群成员缓存前缀
         */
        static final String GROUP_MEMBERS_PREFIX = "group:members:";
        /**
         * 缓存过期时间(秒)
         */
        static final long TTL_SECONDS = 300L;
    }

    /**
     * 分布式锁相关常量
     */
    private static final class LockConstants {
        /**
         * 加入群组锁前缀
         */
        static final String JOIN_PREFIX = "lock:join:";
        /**
         * 邀请成员锁前缀
         */
        static final String INVITE_PREFIX = "lock:invite:";
        /**
         * 退出群组锁前缀
         */
        static final String QUIT_PREFIX = "lock:quit:";
        /**
         * 锁等待时间(秒)
         */
        static final long WAIT_TIME = 5L;
        /**
         * 锁持有时间(秒)
         */
        static final long LEASE_TIME = 10L;
    }
}