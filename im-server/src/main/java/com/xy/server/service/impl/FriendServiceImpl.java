package com.xy.server.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.xy.domain.dto.FriendDto;
import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.FriendVo;
import com.xy.server.api.database.friend.ImFriendFeign;
import com.xy.server.api.database.user.ImUserFeign;
import com.xy.server.service.FriendService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class FriendServiceImpl implements FriendService {

    @Resource
    private ImFriendFeign imFriendFeign;

    @Resource
    private ImUserFeign imUserFeign;

    @Override
    public List<FriendVo> list(String ownerId, Long sequence) {

        // 查询用户的好友关系列表
        List<ImFriendshipPo> friendships = imFriendFeign.selectList(ownerId, sequence);
        if (CollUtil.isEmpty(friendships)) {
            return Collections.emptyList();
        }

        // 转好友id集合
        List<String> ids = friendships.stream().map(ImFriendshipPo::getToId).collect(Collectors.toList());

        // 根据好友id查询好友的用户数据
        Map<String, ImUserDataPo> userMap = imUserFeign.getUserByIds(ids)
                .stream()
                .collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));

        // 将好友信息填充
        return friendships.stream()
                .map(friendship -> {
                    ImUserDataPo user = userMap.get(friendship.getToId());
                    if (user == null) return null;
                    FriendVo vo = new FriendVo();
                    BeanUtils.copyProperties(user, vo);
                    return vo.setUserId(ownerId)
                            .setFriendId(user.getUserId())
                            .setBlack(friendship.getBlack())
                            .setAlias(friendship.getRemark())
                            .setSequence(friendship.getSequence());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public FriendVo getFriendInfo(FriendDto friendDto) {

        String fromId = friendDto.getFromId();

        String toId = friendDto.getToId();

        // 查询好友关系信息
        ImFriendshipPo friendshipPo = imFriendFeign.getOne(fromId, toId);

        // 查询好友用户信息
        ImUserDataPo userDataPo = imUserFeign.getOne(toId);

        FriendVo vo = new FriendVo();
        BeanUtils.copyProperties(userDataPo, vo);

        return vo.setUserId(fromId)
                .setFriendId(userDataPo.getUserId())
                .setBlack(friendshipPo.getBlack())
                .setAlias(friendshipPo.getRemark())
                .setSequence(friendshipPo.getSequence());
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





