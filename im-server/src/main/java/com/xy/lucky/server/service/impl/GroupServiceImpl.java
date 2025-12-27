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
import org.redisson.api.RLockReactive;
import org.redisson.api.RMapCacheReactive;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
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

    private <T> Mono<T> withLock(String key, Mono<T> action, String logDesc) {
        RLockReactive lock = redissonClient.reactive().getLock(key);
        return lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS)
                .flatMap(acquired -> {
                    if (!acquired) {
                        return Mono.error(new MessageException("无法获取锁: " + logDesc));
                    }
                    return action
                            .flatMap(res ->
                                    lock.isHeldByThread(Thread.currentThread().threadId())
                                            .flatMap(held -> held ? lock.unlock() : Mono.empty())
                                            .onErrorResume(e -> Mono.empty())
                                            .thenReturn(res)
                            )
                            .onErrorResume(e ->
                                    lock.isHeldByThread(Thread.currentThread().threadId())
                                            .flatMap(held -> held ? lock.unlock() : Mono.empty())
                                            .onErrorResume(unlockErr -> Mono.empty())
                                            .then(Mono.error(e))
                            );
                });
    }

    @Override
    public Mono<Map<?, ?>> getGroupMembers(GroupDto groupDto) {
        return Mono.fromCallable(() -> {
            String groupId = groupDto.getGroupId();
            List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(groupId);
            if (CollectionUtils.isEmpty(members)) {
                return Collections.emptyMap();
            }
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
            return voMap;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> quitGroup(GroupDto groupDto) {
        String groupId = groupDto.getGroupId();
        String userId = groupDto.getUserId();
        return withLock(QUIT_PREFIX + groupId + ":" + userId, Mono.fromCallable(() -> {
            ImGroupMemberPo member = imGroupMemberDubboService.selectOne(groupId, userId);
            if (member == null) {
                throw new GroupException("用户不在群聊中");
            }
            if (IMemberStatus.GROUP_OWNER.getCode().equals(member.getRole())) {
                throw new GroupException("群主不可退出群聊");
            }
            boolean success = imGroupMemberDubboService.deleteById(member.getGroupMemberId());
            if (success) {
                log.info("退出群聊成功 groupId={} userId={}", groupId, userId);
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then(), "退出群聊 " + groupId);
    }

    public Mono<String> createGroup(@NonNull GroupInviteDto dto) {
        return Mono.fromCallable(() -> {
            if (CollectionUtils.isEmpty(dto.getMemberIds())) {
                throw new GroupException("至少需要一个被邀请人");
            }
            return dto;
        }).flatMap(d -> Mono.fromCallable(() -> imIdDubboService.generateId(
                        IdGeneratorConstant.uuid,
                        IdGeneratorConstant.group_message_id).getStringId())
                .subscribeOn(Schedulers.boundedElastic())
        ).flatMap(groupId -> {
            return Mono.fromCallable(() -> {
                        String groupName = "默认群聊" + IdUtils.randomUUID();
                        long now = DateTimeUtils.getCurrentUTCTimestamp();
                        String ownerId = dto.getUserId();
                        List<String> memberIds = dto.getMemberIds();

                        List<ImGroupMemberPo> members = new ArrayList<>(memberIds.size() + 1);
                        members.add(buildMember(groupId, ownerId, IMemberStatus.GROUP_OWNER, now));
                        memberIds.forEach(id -> members.add(buildMember(groupId, id, IMemberStatus.NORMAL, now)));

                        boolean membersOk = imGroupMemberDubboService.batchInsert(members);
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

                        boolean groupOk = imGroupDubboService.insert(group);
                        if (!groupOk) {
                            throw new GroupException("群信息插入失败");
                        }
                        return group;
                    }).subscribeOn(Schedulers.boundedElastic())
                    .flatMap(group -> generateGroupAvatar(groupId)
                            .onErrorResume(e -> {
                                log.error("生成群头像失败", e);
                                return Mono.empty();
                            })
                            .thenReturn(group)
                    )
                    .flatMap(group -> {
                        RMapCacheReactive<String, Object> groupCache = redissonClient.reactive().getMapCache(GROUP_INFO_PREFIX);
                        return groupCache.fastPut(groupId, group, TTL_SECONDS, TimeUnit.SECONDS).thenReturn(groupId);
                    })
                    .flatMap(gid -> messageService.sendGroupMessage(systemMessage(gid, "已加入群聊,请尽情聊天吧"))
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(gid)
                    );
        });
    }

    public Mono<String> groupInvite(@NonNull GroupInviteDto dto) {
        return Mono.defer(() -> {
            String groupId = StringUtils.hasText(dto.getGroupId()) ? dto.getGroupId() : null;
            Mono<String> groupIdMono = groupId != null ? Mono.just(groupId) : Mono.fromCallable(() -> imIdDubboService.generateId(
                            IdGeneratorConstant.uuid,
                    IdGeneratorConstant.group_message_id).getStringId()).subscribeOn(Schedulers.boundedElastic());

            return groupIdMono.flatMap(gid -> {
                String inviterId = dto.getUserId();
                return withLock(INVITE_PREFIX + gid + ":" + inviterId, Mono.fromCallable(() -> {
                            List<ImGroupMemberPo> existingMembers = imGroupMemberDubboService.selectList(gid);
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

                            ImGroupPo groupPo = imGroupDubboService.selectOne(gid);

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
                            Boolean dbOk = imGroupInviteRequestDubboService.batchInsert(requests);
                            if (!dbOk) {
                                throw new GlobalException(ResultCode.FAIL, "保存邀请请求失败");
                            }
                            return new Object[]{gid, inviterId, newInvitees, groupPo};
                        }).subscribeOn(Schedulers.boundedElastic())
                        .flatMap(res -> {
                            if (res instanceof String) return Mono.just((String) res);
                            Object[] arr = (Object[]) res;
                            String gId = (String) arr[0];
                            String iId = (String) arr[1];
                            List<String> nInvitees = (List<String>) arr[2];
                            ImGroupPo gPo = (ImGroupPo) arr[3];

                            return sendBatchInviteMessages(gId, iId, nInvitees, gPo).thenReturn(gId);
                        }), "群邀请 " + gid);
            });
        });
    }

    @Override
    public Mono<String> inviteGroup(GroupInviteDto dto) {
        Integer type = dto.getType();
        if (IMessageType.CREATE_GROUP.getCode().equals(type)) {
            return createGroup(dto);
        } else if (IMessageType.GROUP_INVITE.getCode().equals(type)) {
            return groupInvite(dto);
        }
        return Mono.error(new GroupException("无效邀请类型"));
    }

    @Override
    public Mono<String> approveGroupInvite(GroupInviteDto dto) {
        return Mono.fromCallable(() -> {
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
            return null;
        }).flatMap(msg -> {
            if (msg != null) return Mono.just(msg);
            String groupId = dto.getGroupId();
            return Mono.fromCallable(() -> imGroupDubboService.selectOne(groupId))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(groupPo -> {
                        if (groupPo == null) return Mono.just("群不存在");
                        if (ImGroupJoinStatus.BAN.getCode().equals(groupPo.getApplyJoinType())) {
                            return Mono.just("群不允许加入");
                        }
                        if (ImGroupJoinStatus.APPROVE.getCode().equals(groupPo.getApplyJoinType())) {
                            return sendJoinApprovalRequestToAdmins(groupId, dto.getInviterId(), dto.getUserId(), groupPo)
                                    .thenReturn("已发送入群验证请求，等待审核");
                        }
                        return processDirectJoin(groupId, dto.getUserId(), dto.getInviterId());
                    });
        });
    }

    private Mono<String> processDirectJoin(String groupId, String userId, String inviterId) {
        return withLock(JOIN_PREFIX + groupId + ":" + userId, Mono.fromCallable(() -> {
                    ImGroupMemberPo member = imGroupMemberDubboService.selectOne(groupId, userId);
            if (member != null && IMemberStatus.NORMAL.getCode().equals(member.getRole())) {
                return "用户已加入群聊";
            }
                    long now = DateTimeUtils.getCurrentUTCTimestamp();
            ImGroupMemberPo newMember = buildMember(groupId, userId, IMemberStatus.NORMAL, now);
                    return imGroupMemberDubboService.batchInsert(List.of(newMember));
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(success -> {
                    if ((Boolean) success) {
                        return updateGroupInfoAndNotify(groupId, inviterId, userId)
                                .thenReturn("成功加入群聊");
                    } else {
                        return Mono.just("加入群聊失败");
            }
                }), "加入群聊 " + groupId);
    }

    private Mono<Void> updateGroupInfoAndNotify(String groupId, String inviterId, String userId) {
        return Mono.fromCallable(() -> imGroupMemberDubboService.selectList(groupId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(updatedMembers -> {
                    if (updatedMembers.size() < 10) {
                        return Mono.fromRunnable(() -> {
                                    ImGroupPo update = new ImGroupPo().setGroupId(groupId);
                                    imGroupDubboService.update(update);
                                }).subscribeOn(Schedulers.boundedElastic())
                                .then(generateGroupAvatar(groupId));
                    }
                    return Mono.empty();
                })
                .then(sendJoinNotification(groupId, inviterId, userId));
    }

    @Override
    public Mono<ImGroupPo> groupInfo(@NonNull GroupDto groupDto) {
        return Mono.defer(() -> {
            RMapCacheReactive<String, Object> cache = redissonClient.reactive().getMapCache(GROUP_INFO_PREFIX);
            return cache.get(groupDto.getGroupId())
                    .map(obj -> (ImGroupPo) obj)
                    .switchIfEmpty(Mono.fromCallable(() -> imGroupDubboService.selectOne(groupDto.getGroupId()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(group -> {
                                if (group != null) {
                                    return cache.fastPut(groupDto.getGroupId(), group, TTL_SECONDS, TimeUnit.SECONDS).thenReturn(group);
                                }
                                return Mono.just(new ImGroupPo());
                            }))
                    ;
        });
    }

    @Override
    public Mono<Boolean> updateGroupInfo(GroupDto groupDto) {
        return Mono.fromCallable(() -> {
            String groupId = groupDto.getGroupId();
            ImGroupPo existingGroup = imGroupDubboService.selectOne(groupId);
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
            boolean success = imGroupDubboService.update(updateGroup);
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
            ImGroupMemberPo member = imGroupMemberDubboService.selectOne(groupId, userId);
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
            boolean success = imGroupMemberDubboService.update(updateMember);
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

    public Mono<Void> generateGroupAvatar(String groupId) {
        return Mono.fromCallable(() -> {
            List<String> avatars = imGroupMemberDubboService.selectNinePeopleAvatar(groupId);
                    return groupHeadImageUtils.getCombinationOfhead(avatars, "defaultGroupHead" + groupId);
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(headFile -> fileService.uploadFile(headFile))
                .flatMap(fileVo -> {
                    String avatarUrl = fileVo.getPath();
                    return Mono.fromCallable(() -> {
                        ImGroupPo update = new ImGroupPo().setGroupId(groupId).setAvatar(avatarUrl);
                        imGroupDubboService.update(update);
                        return avatarUrl;
                    }).subscribeOn(Schedulers.boundedElastic());
                })
                .flatMap(avatarUrl -> {
                    RMapCacheReactive<String, Object> cache = redissonClient.reactive().getMapCache(GROUP_INFO_PREFIX);
                    return cache.get(groupId)
                            .flatMap(obj -> {
                                ImGroupPo cachedGroup = (ImGroupPo) obj;
                                cachedGroup.setAvatar(avatarUrl);
                                return cache.fastPut(groupId, cachedGroup, TTL_SECONDS, TimeUnit.SECONDS);
                            }).then();
                }).onErrorResume(e -> {
                    log.error("异步生成群头像失败 groupId={}", groupId, e);
                    return Mono.empty();
                });
    }

    public Mono<Void> sendBatchInviteMessages(String groupId, String inviterId, List<String> invitees, ImGroupPo groupPo) {
        return Mono.fromCallable(() -> {
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
                        .setRequestId("")
                        .setGroupId(groupId)
                        .setUserId(inviteeId)
                        .setGroupAvatar(groupPo != null ? groupPo.getAvatar() : "")
                        .setGroupName(groupPo != null ? groupPo.getGroupName() : "")
                        .setInviterId(inviterId)
                        .setInviterName(inviterInfo != null ? inviterInfo.getName() : inviterId)
                        .setApproveStatus(0);
                msg.setMessageBody(body);
                messages.add(msg);
                    }
                    return messages;
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .flatMap(msg -> messageService.sendSingleMessage(msg).subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    public Mono<Void> sendJoinNotification(String groupId, String inviterId, String userId) {
        return Mono.fromCallable(() -> {
                    ImUserDataPo invitee = imUserDataDubboService.selectOne(userId);
            ImUserDataPo inviter = imUserDataDubboService.selectOne(inviterId);
            String msg = "\"" + (inviter != null ? inviter.getName() : inviterId) + "\" 邀请 \"" +
                    (invitee != null ? invitee.getName() : userId) + "\" 加入群聊";
                    return msg;
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(msg -> messageService.sendGroupMessage(systemMessage(groupId, msg))).then();
    }

    private Mono<Void> sendJoinApprovalRequestToAdmins(String groupId, String inviterId, String inviteeId, ImGroupPo groupPo) {
        return Mono.fromCallable(() -> {
                    List<ImGroupMemberPo> members = imGroupMemberDubboService.selectList(groupId);
                    if (CollectionUtils.isEmpty(members)) return Collections.<String>emptyList();
                    List<String> adminIds = members.stream()
                    .filter(m -> IMemberStatus.GROUP_OWNER.getCode().equals(m.getRole()) ||
                            IMemberStatus.ADMIN.getCode().equals(m.getRole()))
                    .map(ImGroupMemberPo::getMemberId)
                    .distinct()
                    .collect(Collectors.toList());
                    if (adminIds.isEmpty() && StringUtils.hasText(groupPo.getOwnerId())) {
                adminIds = List.of(groupPo.getOwnerId());
            }
                    return adminIds;
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMap(adminIds -> {
                    if (adminIds.isEmpty()) return Mono.empty();
                    return Mono.zip(
                            Mono.fromCallable(() -> imUserDataDubboService.selectOne(inviterId)).subscribeOn(Schedulers.boundedElastic()),
                            Mono.fromCallable(() -> imUserDataDubboService.selectOne(inviteeId)).subscribeOn(Schedulers.boundedElastic())
                    ).flatMap(tuple -> {
                        ImUserDataPo inviterInfo = tuple.getT1();
                        List<IMSingleMessage> msgs = new ArrayList<>(adminIds.size());
                        for (String adminId : (List<String>) adminIds) {
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
                        return Flux.fromIterable(msgs).flatMap(m -> messageService.sendSingleMessage(m)).then();
                    });
                });
    }
}
