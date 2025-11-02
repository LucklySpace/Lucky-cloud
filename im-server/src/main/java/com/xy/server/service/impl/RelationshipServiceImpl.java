package com.xy.server.service.impl;

import com.xy.core.enums.IMStatus;
import com.xy.domain.dto.FriendDto;
import com.xy.domain.dto.FriendRequestDto;
import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.FriendVo;
import com.xy.domain.vo.FriendshipRequestVo;
import com.xy.domain.vo.GroupVo;
import com.xy.dubbo.api.database.friend.ImFriendshipDubboService;
import com.xy.dubbo.api.database.friend.ImFriendshipRequestDubboService;
import com.xy.dubbo.api.database.group.ImGroupDubboService;
import com.xy.dubbo.api.database.user.ImUserDataDubboService;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.exception.GlobalException;
import com.xy.server.service.RelationshipService;
import com.xy.utils.time.DateTimeUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RelationshipServiceImpl implements RelationshipService {

    private static final String LOCK_READ_CONTACTS_PREFIX = "lock:read:contacts:";
    private static final String LOCK_READ_GROUPS_PREFIX = "lock:read:groups:";
    private static final String LOCK_READ_NEW_FRIENDS_PREFIX = "lock:read:newFriends:";
    private static final String LOCK_ADD_FRIEND_LOCK_PREFIX = "lock:add:friend:";
    private static final String LOCK_APPROVE_FRIEND_LOCK_PREFIX = "lock:approve:friend:";
    private static final String LOCK_DEL_FRIEND_LOCK_PREFIX = "lock:del:friend:";
    private static final String LOCK_UPDATE_FRIEND_REMARK_PREFIX = "lock:update:friend:remark:"; // 新增备注更新锁前缀

    private static final int BATCH_SIZE = 500;
    private static final long LOCK_WAIT_TIME = 5L; // 锁等待时间（秒）
    private static final long LOCK_LEASE_TIME = 10L; // 锁持有时间（秒）

    @DubboReference
    private ImFriendshipDubboService imFriendshipDubboService;

    @DubboReference
    private ImFriendshipRequestDubboService imFriendshipRequestDubboService;

    @DubboReference
    private ImUserDataDubboService imUserDataDubboService;

    @DubboReference
    private ImGroupDubboService imGroupDubboService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 获取联系人列表（加读锁防并发读写冲突）
     */
    @Override
    public Result contacts(String ownerId, Long sequence) {
        long start = System.currentTimeMillis();
        log.debug("contacts() 开始 -> ownerId={}", ownerId);

        RLock readLock = redissonClient.getLock(LOCK_READ_CONTACTS_PREFIX + ownerId);
        try {
            if (!readLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取contacts读锁 ownerId={}", ownerId);
                return Result.success(Collections.emptyList()); // 降级返回空
            }

            List<ImFriendshipPo> friendships = imFriendshipDubboService.selectList(ownerId, sequence);
            if (isEmpty(friendships)) {
                log.debug("没有好友关系 -> ownerId={}, 耗时 {} ms", ownerId, System.currentTimeMillis() - start);
                return Result.success(Collections.emptyList());
            }

            log.info("查询到好友关系数量: {} -> ownerId={}", friendships.size(), ownerId);

            List<String> ids = friendships.stream()
                    .map(ImFriendshipPo::getToId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (ids.isEmpty()) {
                log.warn("所有 friendship 的 toId 都为 null -> ownerId={}, friendshipsCount={}", ownerId, friendships.size());
                return Result.success(Collections.emptyList());
            }

            log.debug("待查询用户 id 数量（去重后）: {}", ids.size());

            List<ImUserDataPo> userDataAll = batchQueryUsers(ids);
            if (userDataAll.isEmpty()) {
                log.warn("未从用户服务中查询到任何用户数据 -> ownerId={}, queriedIdsCount={}", ownerId, ids.size());
                return Result.success(Collections.emptyList());
            }
            log.debug("从用户服务累计查询到用户数据条数: {}", userDataAll.size());

            Map<String, ImUserDataPo> userMap = userDataAll.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            ImUserDataPo::getUserId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));

            List<FriendVo> result = buildFriendVoList(friendships, userMap, ownerId);

            log.info("contacts() 完成 -> ownerId={}, 返回 {} 条，耗时 {} ms",
                    ownerId, result.size(), System.currentTimeMillis() - start);
            return Result.success(result);
        } catch (Exception ex) {
            log.error("contacts() 处理失败 -> ownerId={}, 耗时 {} ms", ownerId, System.currentTimeMillis() - start, ex);
            throw new GlobalException(ResultCode.FAIL, "获取联系人失败");
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 获取群组列表（加读锁）
     */
    @Override
    public Result groups(String userId) {
        long start = System.currentTimeMillis();
        log.debug("groups() 开始 -> userId={}", userId);

        RLock readLock = redissonClient.getLock(LOCK_READ_GROUPS_PREFIX + userId);
        try {
            if (!readLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取groups读锁 userId={}", userId);
                return Result.success(Collections.emptyList());
            }

            List<ImGroupPo> groups = imGroupDubboService.selectList(userId);
            if (isEmpty(groups)) {
                log.debug("未查询到任何群组 -> userId={}, 耗时 {} ms", userId, System.currentTimeMillis() - start);
                return Result.success(Collections.emptyList());
            }
            log.info("查询到群组数: {} -> userId={}", groups.size(), userId);

            List<GroupVo> result = groups.stream()
                    .filter(Objects::nonNull)
                    .map(group -> {
                        GroupVo groupVo = new GroupVo();
                        BeanUtils.copyProperties(group, groupVo);
                        return groupVo;
                    })
                    .collect(Collectors.toList());

            log.info("groups() 完成 -> userId={}, 返回 {} 条，耗时 {} ms",
                    userId, result.size(), System.currentTimeMillis() - start);
            return Result.success(result);
        } catch (Exception ex) {
            log.error("groups() 处理失败 -> userId={}, 耗时 {} ms", userId, System.currentTimeMillis() - start, ex);
            throw new GlobalException(ResultCode.FAIL, "获取群组失败");
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 获取新好友请求（加读锁）
     */
    @Override
    public Result newFriends(String userId) {
        long startMs = System.currentTimeMillis();
        log.debug("开始获取用户的新的好友请求 -> userId={}", userId);

        RLock readLock = redissonClient.getLock(LOCK_READ_NEW_FRIENDS_PREFIX + userId);
        try {
            if (!readLock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取newFriends读锁 userId={}", userId);
                return Result.success(Collections.emptyList());
            }

            List<ImFriendshipRequestPo> requests = imFriendshipRequestDubboService.selectList(userId);
            if (isEmpty(requests)) {
                log.debug("未查询到任何好友请求 -> userId={}，耗时 {} ms", userId, System.currentTimeMillis() - startMs);
                return Result.success(Collections.emptyList());
            }

            log.info("查询到 {} 条好友请求 -> userId={}", requests.size(), userId);

            Set<String> requesterIds = requests.stream()
                    .map(ImFriendshipRequestPo::getFromId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (requesterIds.isEmpty()) {
                log.warn("所有好友请求的 fromId 都为空 -> userId={}, 请求数={}", userId, requests.size());
                return Result.success(Collections.emptyList());
            }

            log.debug("待查询的唯一用户 ID 数量：{} -> ids={}", requesterIds.size(), requesterIds);

            List<ImUserDataPo> userDataList = imUserDataDubboService.selectByIds(new ArrayList<>(requesterIds));
            if (userDataList == null) userDataList = Collections.emptyList();
            log.info("从用户服务查询到 {} 条用户数据", userDataList.size());

            Map<String, ImUserDataPo> userDataMap = userDataList.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            ImUserDataPo::getUserId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));

            List<FriendshipRequestVo> result = buildFriendshipRequestVoList(requests, userDataMap);

            log.info("newFriends 处理完成 -> userId={}, 返回 {} 条，耗时 {} ms",
                    userId, result.size(), System.currentTimeMillis() - startMs);
            return Result.success(result);
        } catch (Exception ex) {
            log.error("获取新的好友请求失败 -> userId={}, 耗时 {} ms", userId, System.currentTimeMillis() - startMs, ex);
            throw new GlobalException(ResultCode.FAIL, "获取新好友失败");
        } finally {
            if (readLock.isHeldByCurrentThread()) {
                readLock.unlock();
            }
        }
    }

    /**
     * 获取好友信息（读操作，无锁）
     */
    @Override
    public Result getFriendInfo(FriendDto friendDto) {
        long start = System.currentTimeMillis();
        if (friendDto == null) {
            throw new IllegalArgumentException("friendDto cannot be null");
        }

        String ownerId = friendDto.getFromId();
        String toId = friendDto.getToId();

        FriendVo vo = new FriendVo();

        try {
            ImUserDataPo userDataPo = imUserDataDubboService.selectOne(toId);
            if (userDataPo == null) {
                vo.setUserId(ownerId).setFriendId(toId).setFlag(IMStatus.NO.getCode());
                log.warn("getFriendInfo: user not found for friendId={}", toId);
                return Result.success(vo);
            }

            BeanUtils.copyProperties(userDataPo, vo);
            vo.setUserId(ownerId).setFriendId(userDataPo.getUserId());

            ImFriendshipPo friendshipPo = imFriendshipDubboService.selectOne(ownerId, toId);
            if (Objects.nonNull(friendshipPo)) {
                vo.setFlag(IMStatus.YES.getCode());
                Optional.ofNullable(friendshipPo.getBlack()).ifPresent(vo::setBlack);
                Optional.ofNullable(friendshipPo.getRemark()).ifPresent(vo::setAlias);
                Optional.ofNullable(friendshipPo.getSequence()).ifPresent(vo::setSequence);
            } else {
                vo.setFlag(IMStatus.NO.getCode());
            }

            log.debug("getFriendInfo 完成 ownerId={} toId={} 耗时:{}ms", ownerId, toId, System.currentTimeMillis() - start);
            return Result.success(vo);
        } catch (Exception ex) {
            log.error("getFriendInfo failed for ownerId={} friendId={}", ownerId, toId, ex);
            vo.setUserId(ownerId).setFriendId(toId).setFlag(IMStatus.NO.getCode());
            throw new GlobalException(ResultCode.FAIL, "获取好友信息失败");
        }
    }

    /**
     * 获取好友信息列表（加读锁防并发）
     */
    @Override
    public Result getFriendInfoList(FriendDto friendDto) {
        long start = System.currentTimeMillis();
        if (friendDto == null) {
            throw new IllegalArgumentException("friendDto cannot be null");
        }
        final String ownerId = friendDto.getFromId();
        final String keyword = friendDto.getKeyword();

        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        try {

            List<ImUserDataPo> users = imUserDataDubboService.search(keyword.trim());
            if (isEmpty(users)) {
                return Result.success(Collections.emptyList());
            }

            List<ImUserDataPo> filteredUsers = users.stream()
                    .filter(u -> u != null && !Objects.equals(u.getUserId(), ownerId))
                    .toList();
            if (filteredUsers.isEmpty()) {
                return Result.success(Collections.emptyList());
            }

            List<String> userIds = filteredUsers.stream()
                    .map(ImUserDataPo::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, ImFriendshipPo> relMap = getFriendshipMap(ownerId, userIds);

            List<FriendVo> result = buildFriendVoLists(filteredUsers, relMap, ownerId);

            log.debug("getFriendInfoList 完成 ownerId={} keyword={} 返回{}条 耗时:{}ms", ownerId, keyword, result.size(), System.currentTimeMillis() - start);
            return Result.success(result);
        } catch (Exception e) {
            log.error("searchFriendsByKeyword failed for ownerId={}, keyword={}", friendDto.getFromId(), friendDto.getKeyword(), e);
            throw new GlobalException(ResultCode.FAIL, "搜索好友失败");
        }
    }


    /**
     * 添加好友（加锁防重复）
     */
    @Override
    public Result addFriend(FriendRequestDto friendRequestDto) {
        long start = System.currentTimeMillis();
        log.debug("addFriend() 开始 -> fromId={}, toId={}", friendRequestDto.getFromId(), friendRequestDto.getToId());

        String lockKey = LOCK_ADD_FRIEND_LOCK_PREFIX + friendRequestDto.getFromId() + ":" + friendRequestDto.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取addFriend锁 fromId={} toId={}", friendRequestDto.getFromId(), friendRequestDto.getToId());
                return Result.failed("添加请求处理中，请稍后重试");
            }

            ImFriendshipRequestPo existingRequests = imFriendshipRequestDubboService.selectOne(
                    new ImFriendshipRequestPo()
                            .setFromId(friendRequestDto.getFromId())
                            .setToId(friendRequestDto.getToId()));

            if (Objects.isNull(existingRequests)) {
                ImFriendshipRequestPo request = createFriendRequest(friendRequestDto);
                imFriendshipRequestDubboService.insert(request);
                log.info("好友请求已创建 -> id={}, fromId={}, toId={}", request.getId(), request.getFromId(), request.getToId());
            } else {
                updateExistingRequest(existingRequests, friendRequestDto);
                imFriendshipRequestDubboService.update(existingRequests);
                log.debug("好友请求已存在，更新信息 -> fromId={}, toId={}", friendRequestDto.getFromId(), friendRequestDto.getToId());
            }

            log.info("addFriend() 完成 -> fromId={}, toId={}, 耗时 {} ms",
                    friendRequestDto.getFromId(), friendRequestDto.getToId(), System.currentTimeMillis() - start);
            return Result.success("添加好友请求成功");
        } catch (Exception ex) {
            log.error("addFriend() 处理失败 -> fromId={}, toId={}, 耗时 {} ms",
                    friendRequestDto.getFromId(), friendRequestDto.getToId(), System.currentTimeMillis() - start, ex);
            throw new GlobalException(ResultCode.FAIL, "添加好友请求失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    /**
     * 批准好友（加锁防并发审批）
     */
    @Override
    public Result approveFriend(FriendRequestDto friendshipRequestDto) {
        long start = System.currentTimeMillis();
        log.debug("approveFriend() 开始 -> requestId={}, approveStatus={}",
                friendshipRequestDto.getId(), friendshipRequestDto.getApproveStatus());

        String lockKey = LOCK_APPROVE_FRIEND_LOCK_PREFIX + friendshipRequestDto.getId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取approveFriend锁 requestId={}", friendshipRequestDto.getId());
                return Result.failed("审批处理中，请稍后重试");
            }

            ImFriendshipRequestPo request = imFriendshipRequestDubboService.selectOne(
                    new ImFriendshipRequestPo().setId(friendshipRequestDto.getId()));

            if (request == null) {
                return Result.failed("好友请求不存在");
            }

            String fromId = request.getFromId();
            String toId = request.getToId();

            if (friendshipRequestDto.getApproveStatus() == 1) {
                createBidirectionalFriendship(fromId, toId, friendshipRequestDto.getRemark());
                log.info("已建立双向好友关系 -> {} <-> {}", fromId, toId);
            }

            imFriendshipRequestDubboService.updateStatus(friendshipRequestDto.getId(), friendshipRequestDto.getApproveStatus());

            log.info("approveFriend() 完成 -> requestId={}, approveStatus={}, 耗时 {} ms",
                    friendshipRequestDto.getId(), friendshipRequestDto.getApproveStatus(), System.currentTimeMillis() - start);
            return Result.success();
        } catch (Exception ex) {
            log.error("approveFriend() 处理失败 -> requestId={}, approveStatus={}, 耗时 {} ms",
                    friendshipRequestDto.getId(), friendshipRequestDto.getApproveStatus(), System.currentTimeMillis() - start, ex);
            throw new GlobalException(ResultCode.FAIL, "审批好友请求失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 删除好友（加锁防重复删除）
     */
    @Override
    public Result delFriend(FriendDto friendDto) {
        long start = System.currentTimeMillis();
        log.debug("delFriend() 开始 -> fromId={}, toId={}", friendDto.getFromId(), friendDto.getToId());

        String lockKey = LOCK_DEL_FRIEND_LOCK_PREFIX + friendDto.getFromId() + ":" + friendDto.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取delFriend锁 fromId={} toId={}", friendDto.getFromId(), friendDto.getToId());
                return Result.failed("删除处理中，请稍后重试");
            }

            String fromId = friendDto.getFromId();
            String toId = friendDto.getToId();

            imFriendshipDubboService.delete(fromId, toId);
            log.info("已删除好友关系 -> {} -> {}", fromId, toId);

            log.info("delFriend() 完成 -> fromId={}, toId={}, 耗时 {} ms",
                    friendDto.getFromId(), friendDto.getToId(), System.currentTimeMillis() - start);
            return Result.success();
        } catch (Exception ex) {
            log.error("delFriend() 处理失败 -> fromId={}, toId={}, 耗时 {} ms",
                    friendDto.getFromId(), friendDto.getToId(), System.currentTimeMillis() - start, ex);
            throw new GlobalException(ResultCode.FAIL, "删除好友失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 修改好友备注（加锁防并发修改）
     */
    @Override
    public Result updateFriendRemark(FriendDto friendDto) {
        long start = System.currentTimeMillis();
        log.debug("updateFriendRemark() 开始 -> ownerId={}, friendId={}", friendDto.getFromId(), friendDto.getToId());

        String lockKey = LOCK_UPDATE_FRIEND_REMARK_PREFIX + friendDto.getFromId() + ":" + friendDto.getToId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
                log.warn("无法获取updateFriendRemark锁 ownerId={} friendId={}", friendDto.getFromId(), friendDto.getToId());
                return Result.failed("备注更新处理中，请稍后重试");
            }

            // 查询好友关系是否存在
            ImFriendshipPo friendshipPo = imFriendshipDubboService.selectOne(friendDto.getFromId(), friendDto.getToId());
            if (friendshipPo == null) {
                return Result.failed("好友关系不存在");
            }

            // 更新备注信息
            friendshipPo.setRemark(friendDto.getKeyword());
            friendshipPo.setSequence(DateTimeUtils.getCurrentUTCTimestamp());
            boolean success = imFriendshipDubboService.update(friendshipPo);

            if (success) {
                log.info("updateFriendRemark() 完成 -> ownerId={}, friendId={}, remark={}, 耗时 {} ms",
                        friendDto.getFromId(), friendDto.getToId(), friendDto.getKeyword(), System.currentTimeMillis() - start);
                return Result.success("备注更新成功");
            } else {
                log.warn("updateFriendRemark() 失败 -> ownerId={}, friendId={}, remark={}, 耗时 {} ms",
                        friendDto.getFromId(), friendDto.getToId(), friendDto.getKeyword(), System.currentTimeMillis() - start);
                return Result.failed("备注更新失败");
            }
        } catch (Exception ex) {
            log.error("updateFriendRemark() 处理失败 -> ownerId={}, friendId={}, remark={}, 耗时 {} ms",
                    friendDto.getFromId(), friendDto.getToId(), friendDto.getKeyword(), System.currentTimeMillis() - start, ex);
            throw new GlobalException(ResultCode.FAIL, "备注更新失败");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 批量查询用户信息（不变）
     */
    private List<ImUserDataPo> batchQueryUsers(List<String> ids) {
        List<ImUserDataPo> userDataAll = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, ids.size());
            List<String> batch = ids.subList(i, end);
            try {
                List<ImUserDataPo> part = imUserDataDubboService.selectByIds(batch);
                if (part != null && !part.isEmpty()) {
                    userDataAll.addAll(part);
                } else {
                    log.debug("分片查询没有返回数据 -> batchSize={}, startIndex={}", batch.size(), i);
                }
            } catch (Exception ex) {
                log.error("分片查询用户失败 -> startIndex={}, batchSize={}", i, batch.size(), ex);
            }
        }
        return userDataAll;
    }

    /**
     * 构建好友VO列表（不变）
     */
    private List<FriendVo> buildFriendVoList(List<ImFriendshipPo> friendships, Map<String, ImUserDataPo> userMap, String ownerId) {
        return friendships.stream()
                .map(friendship -> {
                    ImUserDataPo user = userMap.get(friendship.getToId());
                    if (Objects.isNull(user)) {
                        log.warn("找不到好友对应的用户数据 -> ownerId={}, friendToId={}", ownerId, friendship.getToId());
                        return null;
                    }

                    FriendVo vo = new FriendVo();
                    BeanUtils.copyProperties(user, vo);
                    vo.setUserId(ownerId)
                            .setFriendId(user.getUserId())
                            .setBlack(friendship.getBlack())
                            .setAlias(friendship.getRemark())
                            .setSequence(friendship.getSequence());

                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建好友请求VO列表（不变）
     */
    private List<FriendshipRequestVo> buildFriendshipRequestVoList(List<ImFriendshipRequestPo> requests, Map<String, ImUserDataPo> userDataMap) {
        return requests.stream()
                .map(req -> {
                    FriendshipRequestVo vo = new FriendshipRequestVo();
                    BeanUtils.copyProperties(req, vo);

                    ImUserDataPo userData = userDataMap.get(req.getFromId());
                    if (Objects.nonNull(userData)) {
                        vo.setName(userData.getName());
                        vo.setAvatar(userData.getAvatar());
                    } else {
                        log.warn("无法找到请求者用户数据 -> fromId={}, requestId={}", req.getFromId(), req.getId());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取好友关系映射（不变）
     */
    private Map<String, ImFriendshipPo> getFriendshipMap(String ownerId, List<String> userIds) {
        Map<String, ImFriendshipPo> relMap = new HashMap<>();
        try {
            List<ImFriendshipPo> rels = imFriendshipDubboService.selectByIds(ownerId, userIds);
            if (rels != null) {
                for (ImFriendshipPo r : rels) {
                    if (r != null && r.getToId() != null) {
                        relMap.put(r.getToId(), r);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("批量查询好友关系失败，将使用单条查询. ownerId={}, userIdsCount={}", ownerId, userIds.size(), ex);
        }

        List<String> missingIds = userIds.stream().filter(id -> !relMap.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            for (String fid : missingIds) {
                try {
                    ImFriendshipPo singleRel = imFriendshipDubboService.selectOne(ownerId, fid);
                    if (singleRel != null) relMap.put(fid, singleRel);
                } catch (Exception ex) {
                    log.debug("查询单个好友关系失败 ownerId={} friendId={}", ownerId, fid, ex);
                }
            }
        }

        return relMap;
    }

    /**
     * 构建好友VO列表（用于搜索结果）（不变）
     */
    private List<FriendVo> buildFriendVoLists(List<ImUserDataPo> users, Map<String, ImFriendshipPo> relMap, String ownerId) {
        return users.stream()
                .map(u -> {
                    FriendVo vo = new FriendVo();
                    BeanUtils.copyProperties(u, vo);
                    vo.setUserId(ownerId);
                    vo.setFriendId(u.getUserId());

                    ImFriendshipPo rel = relMap.get(u.getUserId());
                    if (rel != null) {
                        vo.setFlag(IMStatus.YES.getCode());
                        Optional.ofNullable(rel.getBlack()).ifPresent(vo::setBlack);
                        Optional.ofNullable(rel.getRemark()).ifPresent(vo::setAlias);
                        Optional.ofNullable(rel.getSequence()).ifPresent(vo::setSequence);
                    } else {
                        vo.setFlag(IMStatus.NO.getCode());
                    }

                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建好友请求对象（不变）
     */
    private ImFriendshipRequestPo createFriendRequest(FriendRequestDto dto) {
        long currentUTCTimestamp = DateTimeUtils.getCurrentUTCTimestamp();
        ImFriendshipRequestPo request = new ImFriendshipRequestPo();
        request.setId(UUID.randomUUID().toString());
        request.setFromId(dto.getFromId());
        request.setToId(dto.getToId());
        request.setMessage(dto.getMessage());
        request.setApproveStatus(0);
        request.setReadStatus(0);
        request.setDelFlag(1);
        request.setRemark(dto.getRemark());
        request.setUpdateTime(currentUTCTimestamp);
        request.setCreateTime(currentUTCTimestamp);
        return request;
    }

    /**
     * 更新已存在的请求（不变）
     */
    private void updateExistingRequest(ImFriendshipRequestPo existing, FriendRequestDto dto) {
        existing.setMessage(dto.getMessage());
        existing.setRemark(dto.getRemark());
        existing.setUpdateTime(DateTimeUtils.getCurrentUTCTimestamp());
    }

    /**
     * 创建双向好友关系（不变）
     */
    private void createBidirectionalFriendship(String fromId, String toId, String remark) {

        long currentUTCTimestamp = DateTimeUtils.getCurrentUTCTimestamp();

        ImFriendshipPo friendship1 = new ImFriendshipPo();
        friendship1.setOwnerId(fromId);
        friendship1.setToId(toId);
        friendship1.setRemark(remark);
        friendship1.setDelFlag(1);
        friendship1.setBlack(1);
        friendship1.setSequence(currentUTCTimestamp);
        friendship1.setCreateTime(currentUTCTimestamp);

        ImFriendshipPo friendship2 = new ImFriendshipPo();
        friendship2.setOwnerId(toId);
        friendship2.setToId(fromId);
        friendship2.setRemark("");
        friendship2.setDelFlag(1);
        friendship2.setBlack(1);
        friendship2.setSequence(currentUTCTimestamp);
        friendship2.setCreateTime(currentUTCTimestamp);

        imFriendshipDubboService.insert(friendship1);
        imFriendshipDubboService.insert(friendship2);
    }

    /**
     * 判断集合是否为空（不变）
     */
    private boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}