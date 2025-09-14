package com.xy.server.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.xy.domain.dto.FriendDto;
import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.FriendVo;
import com.xy.domain.vo.FriendshipRequestVo;
import com.xy.domain.vo.GroupVo;
import com.xy.server.api.database.friend.ImRelationshipFeign;
import com.xy.server.api.database.user.ImUserFeign;
import com.xy.server.service.RelationshipService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RelationshipServiceImpl implements RelationshipService {

    @Resource
    private ImRelationshipFeign imRelationshipFeign;

    @Resource
    private ImUserFeign imUserFeign;

    private static final int BATCH_SIZE = 500;

    @Override
    public List<FriendVo> contacts(String ownerId) {
        long start = System.currentTimeMillis();
        log.debug("contacts() 开始 -> ownerId={}", ownerId);

        try {
            // 1. 查询好友关系（可能返回 null）
            List<ImFriendshipPo> friendships = imRelationshipFeign.contacts(ownerId);

            if (friendships == null || friendships.isEmpty()) {
                log.debug("没有好友关系 -> ownerId={}, 耗时 {} ms", ownerId, System.currentTimeMillis() - start);
                return Collections.emptyList();
            }

            log.info("查询到好友关系数量: {} -> ownerId={}", friendships.size(), ownerId);

            // 2. 提取 friendId 列表（这里使用 getToId，遵循原代码语义），并去重且保留顺序
            List<String> ids = friendships.stream()
                    .map(ImFriendshipPo::getToId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());

            if (ids.isEmpty()) {
                log.warn("所有 friendship 的 toId 都为 null -> ownerId={}, friendshipsCount={}", ownerId, friendships.size());
                return Collections.emptyList();
            }

            log.debug("待查询用户 id 数量（去重后）: {}", ids.size());

            // 3. 批量分片查询用户数据（分片以防 URL 过长 / 参数过多 / 单次响应过大）
            List<ImUserDataPo> userDataAll = new ArrayList<>();
            for (int i = 0; i < ids.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, ids.size());
                List<String> batch = ids.subList(i, end);
                try {
                    List<ImUserDataPo> part = imUserFeign.getUserByIds(batch);
                    if (part != null && !part.isEmpty()) {
                        userDataAll.addAll(part);
                    } else {
                        log.debug("分片查询没有返回数据 -> batchSize={}, startIndex={}", batch.size(), i);
                    }
                } catch (Exception ex) {
                    // 单片错误不应阻断整体流程，记录后继续下一片
                    log.error("分片查询用户失败 -> startIndex={}, batchSize={}", i, batch.size(), ex);
                }
            }
            if (userDataAll.isEmpty()) {
                log.warn("未从用户服务中查询到任何用户数据 -> ownerId={}, queriedIdsCount={}", ownerId, ids.size());
                return Collections.emptyList();
            }
            log.debug("从用户服务累计查询到用户数据条数: {}", userDataAll.size());

            // 4. 转为 Map 便于按 friendId 查找。遇到重复 userId 保留首个（可按需求改）。
            Map<String, ImUserDataPo> userMap = userDataAll.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            ImUserDataPo::getUserId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));

            // 5. 构造 FriendVo 列表，保留原 friendships 的顺序（如果需要）
            List<FriendVo> result = friendships.stream()
                    .map(friendship -> {
                        ImUserDataPo user = userMap.get(friendship.getToId());

                        if (Objects.isNull(user)) {
                            // 找不到用户数据则记录并在最终结果中过滤掉（与原实现相同）
                            log.warn("找不到好友对应的用户数据 -> ownerId={}, friendToId={}",
                                    ownerId, friendship.getToId());
                            return null;
                        }

                        FriendVo vo = new FriendVo();
                        // 通常我们希望把用户基本信息（昵称、头像等）拷贝到 VO
                        BeanUtils.copyProperties(user, vo);
                        // 补充好友关系相关字段（ownerId 固定为当前用户）
                        vo.setUserId(ownerId)
                                .setFriendId(user.getUserId())     // 好友的 userId
                                .setBlack(friendship.getBlack())   // 黑名单标识
                                .setAlias(friendship.getRemark())  // 备注/别名
                                .setSequence(friendship.getSequence()); // 顺序/序列号

                        return vo;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("contacts() 完成 -> ownerId={}, 返回 {} 条，耗时 {} ms",
                    ownerId, result.size(), System.currentTimeMillis() - start);
            return result;
        } catch (Exception ex) {
            log.error("contacts() 处理失败 -> ownerId={}, 耗时 {} ms", ownerId, System.currentTimeMillis() - start, ex);
            return Collections.emptyList();
        }
    }

    @Override
    public List<GroupVo> groups(String userId) {
        long start = System.currentTimeMillis();
        log.debug("groups() 开始 -> userId={}", userId);

        try {
            // 1. 查询群组（可能为 null）
            List<ImGroupPo> groups = imRelationshipFeign.group(userId);
            if (groups == null || groups.isEmpty()) {
                log.debug("未查询到任何群组 -> userId={}, 耗时 {} ms", userId, System.currentTimeMillis() - start);
                return Collections.emptyList();
            }
            log.info("查询到群组数: {} -> userId={}", groups.size(), userId);

            // 2. 转换为 GroupVo。若需要后续丰富群信息（成员数、在线数等），可以在这里批量调用群服务继续填充。
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
            return result;
        } catch (Exception ex) {
            log.error("groups() 处理失败 -> userId={}, 耗时 {} ms", userId, System.currentTimeMillis() - start, ex);
            return Collections.emptyList();
        }
    }
    @Override
    public List<FriendshipRequestVo> newFriends(String userId) {

        long startMs = System.currentTimeMillis();

        log.debug("开始获取用户的新的好友请求 -> userId={}", userId);

        try {
            // 1) 从关系服务获取好友请求列表（可能为 null）
            List<ImFriendshipRequestPo> requests = imRelationshipFeign.newFriends(userId);

            if (requests == null || requests.isEmpty()) {
                log.debug("未查询到任何好友请求 -> userId={}，耗时 {} ms", userId, System.currentTimeMillis() - startMs);
                return Collections.emptyList();
            }

            log.info("查询到 {} 条好友请求 -> userId={}", requests.size(), userId);

            // 2) 提取所有请求者的 fromId（请求发送方）并去重
            //    注意：原始实现提取的是 getToId，但随后按 getFromId 查找用户，存在逻辑不一致（已修正为 getFromId）。
            Set<String> requesterIds = requests.stream()
                    .map(ImFriendshipRequestPo::getFromId)  // 取请求发起人 ID
                    .filter(Objects::nonNull)               // 过滤 null
                    .collect(Collectors.toCollection(LinkedHashSet::new)); // 保持插入顺序且去重

            if (requesterIds.isEmpty()) {
                log.warn("所有好友请求的 fromId 都为空 -> userId={}, 请求数={}", userId, requests.size());
                return Collections.emptyList();
            }

            log.debug("待查询的唯一用户 ID 数量：{} -> ids={}", requesterIds.size(), requesterIds);

            // 3) 批量查询用户数据（imUserFeign.getUserByIds 应该支持批量查询）
            List<ImUserDataPo> userDataList = imUserFeign.getUserByIds(new ArrayList<>(requesterIds));

            if (userDataList == null) userDataList = Collections.emptyList();
            log.info("从用户服务查询到 {} 条用户数据", userDataList.size());

            // 4) 将用户列表转为 Map 以便快速查找：userId -> ImUserDataPo
            //    如果存在重复 userId，保留第一个（可按需求改为最后一个）
            Map<String, ImUserDataPo> userDataMap = userDataList.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            ImUserDataPo::getUserId,
                            Function.identity(),
                            (existing, replacement) -> existing // 遇到重复 key 时保留 existing
                    ));

            // 5) 构建结果，保留原请求信息并填充 user 昵称、头像等
            List<FriendshipRequestVo> result = requests.stream()
                    .map(req -> {

                        FriendshipRequestVo vo = new FriendshipRequestVo();
                        // 复制基本字段（request -> VO）
                        BeanUtils.copyProperties(req, vo);

                        // 用 fromId 去查找对应的用户信息
                        ImUserDataPo userData = userDataMap.get(req.getFromId());

                        if (Objects.nonNull(userData)) {
                            // 仅覆盖需要显示的用户字段（避免覆盖 request 的其他字段）
                            vo.setName(userData.getName());
                            vo.setAvatar(userData.getAvatar());
                        } else {
                            // 记录警告：有请求但没有找到对应用户信息
                            log.warn("无法找到请求者用户数据 -> fromId={}, requestId={}", req.getFromId(), req.getId());
                        }

                        return vo;
                    }).collect(Collectors.toList());

            log.info("newFriends 处理完成 -> userId={}, 返回 {} 条，耗时 {} ms",
                    userId, result.size(), System.currentTimeMillis() - startMs);
            return result;
        } catch (Exception ex) {
            // 友好降级：记录错误并返回空列表，避免上层直接因异常失败（视业务需要也可以抛出自定义异常）
            log.error("获取新的好友请求失败 -> userId={}, 耗时 {} ms", userId, System.currentTimeMillis() - startMs, ex);
            return Collections.emptyList();
        }
    }

    @Override
    public FriendVo getFriendInfo(FriendDto friendDto) {

        String ownerId = friendDto.getFromId();

        String friendId = friendDto.getToId();

        // 查询好友关系信息
        ImFriendshipPo friendshipPo = imRelationshipFeign.getOne(ownerId, friendId);

        // 查询好友用户信息
        ImUserDataPo userDataPo = imUserFeign.getOne(friendId);

        FriendVo vo = new FriendVo();
        BeanUtils.copyProperties(userDataPo, vo);

        return vo.setUserId(ownerId)
                .setFriendId(userDataPo.getUserId());
//                .setBlack(friendshipPo.getBlack())
//                .setAlias(friendshipPo.getRemark())
                //.setSequence(friendshipPo.getSequence());
    }

    //
//    @Override
//    public FriendVo findFriend(FriendDto friendDto) {
//
//        QueryWrapper<ImFriendshipPo> imFriendshipQuery = new QueryWrapper<>();
//        imFriendshipQuery.eq("owner_id", friendDto.getFromId());
//        imFriendshipQuery.eq("to_id", friendDto.getToId());
//        ImFriendshipPo imFriendshipPo = imFriendshipMapper.selectOne(imFriendshipQuery);
//
//        FriendVo friendVo = new FriendVo();
//
//        QueryWrapper<ImUserDataPo> imUserDataQuery = new QueryWrapper<>();
//        imUserDataQuery.eq("user_id", friendDto.getToId());
//        ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(imUserDataQuery);
//
//        if (ObjectUtil.isNotEmpty(imUserDataPo)) {
//            BeanUtils.copyProperties(imUserDataPo, friendVo);
//        } else {
//           return null;
//        }
//
//        if (ObjectUtil.isNotEmpty(imFriendshipPo)) {
//            friendVo.setFlag(IMStatus.YES.getCode());
//        } else {
//            friendVo.setFlag(IMStatus.NO.getCode());
//        }
//
//        return friendVo;
//    }
//
//    @Override
//    @Transactional
//    public void addFriend(FriendRequestDto friendRequestDto) {
//
//        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
//
//        imFriendshipRequestQuery.eq("from_id", friendRequestDto.getFromId())
//                .eq("to_id", friendRequestDto.getToId());
//
//        if (ObjectUtil.isEmpty(imFriendshipRequestMapper.selectOne(imFriendshipRequestQuery))) {
//
//            ImFriendshipRequestPo imFriendshipRequestPo = new ImFriendshipRequestPo();
//
//            String id = UUID.randomUUID().toString();
//
//            BeanUtil.copyProperties(friendRequestDto, imFriendshipRequestPo);
//
//            imFriendshipRequestPo.setId(id)
//                    .setReadStatus(IMStatus.NO.getCode())
//                    .setApproveStatus(IMStatus.NO.getCode())
//            ;
//
//            imFriendshipRequestMapper.insert(imFriendshipRequestPo);
//        }
//    }
//
//    @Override
//    public List<FriendshipRequestVo> request(String userId) {
//
//        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
//
//        imFriendshipRequestQuery.eq("to_id", userId);
//
//        List<ImFriendshipRequestPo> imFriendshipRequestPoList = imFriendshipRequestMapper.selectList(imFriendshipRequestQuery);
//
//        List<FriendshipRequestVo> friendshipRequestVoList = new ArrayList<>();
//
//        imFriendshipRequestPoList.stream().forEach(imFriendshipRequestPo -> {
//
//            FriendshipRequestVo FriendshipRequestVo = new FriendshipRequestVo();
//
//            BeanUtils.copyProperties(imFriendshipRequestPo, FriendshipRequestVo);
//
//            QueryWrapper<ImUserDataPo> imUserDataQuery = new QueryWrapper<>();
//            imUserDataQuery.eq("user_id", imFriendshipRequestPo.getToId());
//            ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(imUserDataQuery);
//
//            FriendshipRequestVo
//                    .setName(imUserDataPo.getName())
//                    .setAvatar(imUserDataPo.getAvatar());
//
//            friendshipRequestVoList.add(FriendshipRequestVo);
//        });
//
//        return friendshipRequestVoList;
//    }
//
//    @Override
//    @Transactional
//    public void approveFriend(FriendshipRequestDto friendshipRequestDto) {
//
//        String fromId = friendshipRequestDto.getFromId();
//
//        String toId = friendshipRequestDto.getToId();
//
//        // 被申请方审批通过构建好友关系
//        if (friendshipRequestDto.getApproveStatus().equals(IMStatus.YES.getCode())) {
//
//            QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
//
//            imFriendshipRequestQuery.eq("id", friendshipRequestDto.getId());
//
//            // 查询申请方备注
//            ImFriendshipRequestPo imFriendshipRequestPo = imFriendshipRequestMapper.selectOne(imFriendshipRequestQuery);
//
//            bindFriend(fromId, toId, imFriendshipRequestPo.getRemark());
//
//            bindFriend(toId, fromId, friendshipRequestDto.getRemark());
//
//            imFriendshipRequestPo
//                    .setReadStatus(IMStatus.YES.getCode())
//                    .setApproveStatus(IMStatus.YES.getCode())
//            ;
//
//            imFriendshipRequestMapper.updateById(imFriendshipRequestPo);
//        }
//    }
//
//
//    /**
//     * 建立好友关系
//     *
//     * @param fromId 申请方
//     * @param toId   被申请方
//     * @param remark 备注
//     */
//    public void bindFriend(String fromId, String toId, String remark) {
//
//        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();
//
//        queryWrapper.lambda().eq(ImFriendshipPo::getOwnerId, fromId).eq(ImFriendshipPo::getToId, toId);
//
//        if (imFriendshipMapper.selectList(queryWrapper).size() == 0) {
//
//            // 申请方对象
//            ImFriendshipPo fromFriendship = new ImFriendshipPo()
//                    .setOwnerId(fromId)
//                    .setToId(toId)
//                    .setRemark(remark)
//                    .setStatus(IMStatus.YES.getCode())
//                    .setBlack(IMStatus.YES.getCode())
//                    .setSequence(new Date().getTime());
//
//            imFriendshipMapper.insert(fromFriendship);
//        }
//    }
//
//
//    /**
//     * 删除好友，单向解除好友关系
//     *
//     * @param friendDto
//     */
//    @Override
//    public void delFriend(FriendDto friendDto) {
//
//        String fromId = friendDto.getFromId();
//
//        String toId = friendDto.getToId();
//
//        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();
//
//        queryWrapper.lambda().eq(ImFriendshipPo::getOwnerId, fromId).eq(ImFriendshipPo::getToId, toId);
//
//        if (imFriendshipMapper.selectList(queryWrapper).size() > 0) {
//
//            imFriendshipMapper.delete(queryWrapper);
//        }
//    }


}





