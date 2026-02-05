package com.xy.lucky.server.service.impl;

import com.xy.lucky.api.friend.ImFriendshipDubboService;
import com.xy.lucky.api.friend.ImFriendshipRequestDubboService;
import com.xy.lucky.api.group.ImGroupDubboService;
import com.xy.lucky.api.user.ImUserDataDubboService;
import com.xy.lucky.core.enums.IMApproveStatus;
import com.xy.lucky.core.enums.IMStatus;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.server.common.LockExecutor;
import com.xy.lucky.server.domain.dto.FriendDto;
import com.xy.lucky.server.domain.dto.FriendRequestDto;
import com.xy.lucky.server.domain.mapper.FriendRequestBeanMapper;
import com.xy.lucky.server.domain.mapper.GroupBeanMapper;
import com.xy.lucky.server.domain.mapper.UserDataBeanMapper;
import com.xy.lucky.server.domain.vo.FriendVo;
import com.xy.lucky.server.domain.vo.FriendshipRequestVo;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.RelationshipService;
import com.xy.lucky.utils.time.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户关系服务实现
 *
 * @author xy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationshipServiceImpl implements RelationshipService {

    /**
     * 分布式锁前缀
     */
    private static final String LOCK_PREFIX = "lock:relationship:";
    /**
     * 批量查询大小
     */
    private static final int BATCH_SIZE = 500;
    private final LockExecutor lockExecutor;
    @DubboReference
    private ImFriendshipDubboService friendshipDubboService;
    @DubboReference
    private ImFriendshipRequestDubboService friendshipRequestDubboService;
    @DubboReference
    private ImUserDataDubboService userDataDubboService;
    @DubboReference
    private ImGroupDubboService groupDubboService;

    /**
     * 获取联系人列表
     *
     * @param ownerId  所有者ID（已在 Controller 层校验）
     * @param sequence 序列号
     * @return 好友列表
     */
    @Override
    public List<?> contacts(String ownerId, Long sequence) {
        return lockExecutor.execute(LOCK_PREFIX + "contacts:" + ownerId, () -> {
            List<ImFriendshipPo> friendships = friendshipDubboService.queryList(ownerId, sequence);
            if (CollectionUtils.isEmpty(friendships)) {
                return Collections.emptyList();
            }

            List<String> friendIds = friendships.stream()
                    .map(ImFriendshipPo::getToId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (friendIds.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, ImUserDataPo> userMap = batchQueryUserMap(friendIds);
            if (userMap.isEmpty()) {
                return Collections.emptyList();
            }

            return buildFriendVoList(friendships, userMap, ownerId);
        });
    }

    /**
     * 获取群组列表
     *
     * @param userId 用户ID（已在 Controller 层校验）
     * @return 群组列表
     */
    @Override
    public List<?> groups(String userId) {
        return lockExecutor.execute(LOCK_PREFIX + "groups:" + userId, () -> {
            List<ImGroupPo> groups = groupDubboService.queryList(userId);
            if (CollectionUtils.isEmpty(groups)) {
                return Collections.emptyList();
            }

            return groups.stream()
                    .filter(Objects::nonNull)
                    .map(GroupBeanMapper.INSTANCE::toGroupVo)
                    .toList();
        });
    }

    /**
     * 获取新好友请求列表
     *
     * @param userId 用户ID（已在 Controller 层校验）
     * @return 好友请求列表
     */
    @Override
    public List<?> newFriends(String userId) {
        return lockExecutor.execute(LOCK_PREFIX + "newFriends:" + userId, () -> {
            List<ImFriendshipRequestPo> requests = friendshipRequestDubboService.queryList(userId);
            if (CollectionUtils.isEmpty(requests)) {
                return Collections.emptyList();
            }

            Set<String> requesterIds = requests.stream()
                    .map(ImFriendshipRequestPo::getFromId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (requesterIds.isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, ImUserDataPo> userMap = queryUserMap(new ArrayList<>(requesterIds));
            return buildFriendshipRequestVoList(requests, userMap);
        });
    }

    /**
     * 获取好友信息
     *
     * @param dto 查询条件（已在 Controller 层校验）
     * @return 好友信息
     */
    @Override
    public FriendVo getFriendInfo(FriendDto dto) {
        String ownerId = dto.getFromId();
        String toId = dto.getToId();

        ImUserDataPo userData = userDataDubboService.queryOne(toId);
        if (userData == null) {
            throw new MessageException("用户不存在");
        }

        FriendVo vo = UserDataBeanMapper.INSTANCE.toFriendVo(userData);
        vo.setUserId(ownerId).setFriendId(userData.getUserId());

        ImFriendshipPo friendship = friendshipDubboService.queryOne(ownerId, toId);
        if (friendship != null) {
            vo.setFlag(IMStatus.YES.getCode());
            Optional.ofNullable(friendship.getBlack()).ifPresent(vo::setBlack);
            Optional.ofNullable(friendship.getRemark()).ifPresent(vo::setAlias);
            Optional.ofNullable(friendship.getSequence()).ifPresent(vo::setSequence);
        } else {
            vo.setFlag(IMStatus.NO.getCode());
        }

        return vo;
    }

    /**
     * 搜索好友
     *
     * @param dto 搜索条件
     * @return 搜索结果列表
     */
    @Override
    public List<?> getFriendInfoList(FriendDto dto) {
        String keyword = dto.getKeyword();
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        String ownerId = dto.getFromId();

        List<ImUserDataPo> users = userDataDubboService.queryByKeyword(keyword.trim());
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyList();
        }

        List<ImUserDataPo> filteredUsers = users.stream()
                .filter(u -> u != null && !Objects.equals(u.getUserId(), ownerId))
                .toList();

        if (filteredUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> userIds = filteredUsers.stream()
                .map(ImUserDataPo::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, ImFriendshipPo> friendshipMap = queryFriendshipMap(ownerId, userIds);
        return buildSearchResultVoList(filteredUsers, friendshipMap, ownerId);
    }

    /**
     * 添加好友请求
     *
     * @param dto 好友请求信息（已在 Controller 层校验）
     * @return 处理结果
     */
    @Override
    public String addFriend(FriendRequestDto dto) {
        String lockKey = LOCK_PREFIX + "add:" + dto.getFromId() + ":" + dto.getToId();
        return lockExecutor.execute(lockKey, () -> {
            ImFriendshipRequestPo existing = friendshipRequestDubboService.queryOne(
                    new ImFriendshipRequestPo().setFromId(dto.getFromId()).setToId(dto.getToId()));

            if (existing == null) {
                ImFriendshipRequestPo request = buildFriendRequest(dto);
                friendshipRequestDubboService.creat(request);
                log.info("创建好友请求: from={}, to={}", dto.getFromId(), dto.getToId());
            } else {
                existing.setMessage(dto.getMessage());
                existing.setRemark(dto.getRemark());
                existing.setUpdateTime(DateTimeUtils.getCurrentUTCTimestamp());
                friendshipRequestDubboService.modify(existing);
                log.debug("更新好友请求: from={}, to={}", dto.getFromId(), dto.getToId());
            }

            return "添加好友请求成功";
        });
    }

    /**
     * 审批好友请求
     *
     * @param dto 审批信息（已在 Controller 层校验）
     */
    @Override
    public void approveFriend(FriendRequestDto dto) {
        String lockKey = LOCK_PREFIX + "approve:" + dto.getId();
        lockExecutor.execute(lockKey, () -> {
            ImFriendshipRequestPo request = friendshipRequestDubboService.queryOne(
                    new ImFriendshipRequestPo().setId(dto.getId()));

            if (request == null) {
                throw new MessageException("好友请求不存在");
            }

            if (IMApproveStatus.APPROVED.getCode().equals(dto.getApproveStatus())) {
                createBidirectionalFriendship(request.getFromId(), request.getToId(), dto.getRemark());
                log.info("建立好友关系: {} <-> {}", request.getFromId(), request.getToId());
            }

            friendshipRequestDubboService.modifyStatus(dto.getId(), dto.getApproveStatus());
        });
    }

    /**
     * 删除好友
     *
     * @param dto 好友信息（已在 Controller 层校验）
     */
    @Override
    public void delFriend(FriendDto dto) {
        String lockKey = LOCK_PREFIX + "delete:" + dto.getFromId() + ":" + dto.getToId();
        lockExecutor.execute(lockKey, () -> {
            friendshipDubboService.removeOne(dto.getFromId(), dto.getToId());
            log.info("删除好友: {} -> {}", dto.getFromId(), dto.getToId());
        });
    }

    /**
     * 更新好友备注
     *
     * @param dto 备注信息（已在 Controller 层校验）
     * @return 是否成功
     */
    @Override
    public Boolean updateFriendRemark(FriendDto dto) {
        String lockKey = LOCK_PREFIX + "remark:" + dto.getFromId() + ":" + dto.getToId();
        return lockExecutor.execute(lockKey, () -> {
            ImFriendshipPo friendship = friendshipDubboService.queryOne(dto.getFromId(), dto.getToId());
            if (friendship == null) {
                throw new MessageException("好友关系不存在");
            }

            friendship.setRemark(dto.getRemark());
            friendship.setSequence(DateTimeUtils.getCurrentUTCTimestamp());

            if (!friendshipDubboService.modify(friendship)) {
                throw new MessageException("更新好友备注失败");
            }

            log.info("更新好友备注: {} -> {}, remark={}", dto.getFromId(), dto.getToId(), dto.getRemark());
            return true;
        });
    }

    // ==================== 私有方法 ====================

    /**
     * 批量查询用户信息
     */
    private Map<String, ImUserDataPo> batchQueryUserMap(List<String> userIds) {
        List<ImUserDataPo> allUsers = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, userIds.size());
            List<String> batch = userIds.subList(i, end);
            try {
                List<ImUserDataPo> users = userDataDubboService.queryListByIds(batch);
                if (!CollectionUtils.isEmpty(users)) {
                    allUsers.addAll(users);
                }
            } catch (Exception e) {
                log.error("批量查询用户失败: start={}, size={}", i, batch.size(), e);
            }
        }

        return allUsers.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ImUserDataPo::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing));
    }

    /**
     * 查询用户信息映射
     */
    private Map<String, ImUserDataPo> queryUserMap(List<String> userIds) {
        List<ImUserDataPo> users = userDataDubboService.queryListByIds(userIds);
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }

        return users.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ImUserDataPo::getUserId,
                        Function.identity(),
                        (existing, replacement) -> existing));
    }

    /**
     * 查询好友关系映射
     */
    private Map<String, ImFriendshipPo> queryFriendshipMap(String ownerId, List<String> userIds) {
        Map<String, ImFriendshipPo> result = new HashMap<>();

        try {
            List<ImFriendshipPo> friendships = friendshipDubboService.queryListByIds(ownerId, userIds);
            if (friendships != null) {
                for (ImFriendshipPo f : friendships) {
                    if (f != null && f.getToId() != null) {
                        result.put(f.getToId(), f);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量查询好友关系失败，使用单条查询: ownerId={}", ownerId, e);
        }

        List<String> missingIds = userIds.stream()
                .filter(id -> !result.containsKey(id))
                .toList();

        for (String friendId : missingIds) {
            try {
                ImFriendshipPo friendship = friendshipDubboService.queryOne(ownerId, friendId);
                if (friendship != null) {
                    result.put(friendId, friendship);
                }
            } catch (Exception e) {
                log.debug("查询单个好友关系失败: ownerId={}, friendId={}", ownerId, friendId);
            }
        }

        return result;
    }

    /**
     * 构建好友 VO 列表
     */
    private List<FriendVo> buildFriendVoList(List<ImFriendshipPo> friendships,
                                             Map<String, ImUserDataPo> userMap, String ownerId) {
        return friendships.stream()
                .map(friendship -> {
                    ImUserDataPo user = userMap.get(friendship.getToId());
                    if (user == null) {
                        return null;
                    }

                    FriendVo vo = UserDataBeanMapper.INSTANCE.toFriendVo(user);
                    vo.setUserId(ownerId)
                            .setFriendId(user.getUserId())
                            .setBlack(friendship.getBlack())
                            .setAlias(friendship.getRemark())
                            .setSequence(friendship.getSequence());
                    return vo;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 构建好友请求 VO 列表
     */
    private List<FriendshipRequestVo> buildFriendshipRequestVoList(List<ImFriendshipRequestPo> requests,
                                                                   Map<String, ImUserDataPo> userMap) {
        return requests.stream()
                .map(request -> {
                    FriendshipRequestVo vo = FriendRequestBeanMapper.INSTANCE.toFriendshipRequestVo(request);
                    ImUserDataPo user = userMap.get(request.getFromId());
                    if (user != null) {
                        vo.setName(user.getName());
                        vo.setAvatar(user.getAvatar());
                    }
                    return vo;
                })
                .toList();
    }

    /**
     * 构建搜索结果 VO 列表
     */
    private List<FriendVo> buildSearchResultVoList(List<ImUserDataPo> users,
                                                   Map<String, ImFriendshipPo> friendshipMap, String ownerId) {
        return users.stream()
                .map(user -> {
                    FriendVo vo = UserDataBeanMapper.INSTANCE.toFriendVo(user);
                    vo.setUserId(ownerId);
                    vo.setFriendId(user.getUserId());

                    ImFriendshipPo friendship = friendshipMap.get(user.getUserId());
                    if (friendship != null) {
                        vo.setFlag(IMStatus.YES.getCode());
                        Optional.ofNullable(friendship.getBlack()).ifPresent(vo::setBlack);
                        Optional.ofNullable(friendship.getRemark()).ifPresent(vo::setAlias);
                        Optional.ofNullable(friendship.getSequence()).ifPresent(vo::setSequence);
                    } else {
                        vo.setFlag(IMStatus.NO.getCode());
                    }

                    return vo;
                })
                .toList();
    }

    /**
     * 构建好友请求
     */
    private ImFriendshipRequestPo buildFriendRequest(FriendRequestDto dto) {
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        return new ImFriendshipRequestPo()
                .setId(UUID.randomUUID().toString())
                .setFromId(dto.getFromId())
                .setToId(dto.getToId())
                .setMessage(dto.getMessage())
                .setRemark(dto.getRemark())
                .setApproveStatus(IMApproveStatus.PENDING.getCode())
                .setReadStatus(IMessageReadStatus.UNREAD.getCode());
    }

    /**
     * 创建双向好友关系
     */
    private void createBidirectionalFriendship(String fromId, String toId, String remark) {
        long now = DateTimeUtils.getCurrentUTCTimestamp();

        ImFriendshipPo friendship1 = new ImFriendshipPo()
                .setOwnerId(fromId)
                .setToId(toId)
                .setRemark(remark)
                .setBlack(IMStatus.YES.getCode())
                .setSequence(now);

        ImFriendshipPo friendship2 = new ImFriendshipPo()
                .setOwnerId(toId)
                .setToId(fromId)
                .setRemark("")
                .setBlack(IMStatus.YES.getCode())
                .setSequence(now);

        friendshipDubboService.creat(friendship1);
        friendshipDubboService.creat(friendship2);
    }
}
