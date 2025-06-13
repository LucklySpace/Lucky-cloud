package com.xy.server.service.impl;

import com.xy.server.service.FriendService;
import org.springframework.stereotype.Service;


@Service
public class FriendServiceImpl implements FriendService {
//
//    @Resource
//    private ImFriendshipMapper imFriendshipMapper;
//
//    @Resource
//    private ImFriendshipRequestMapper imFriendshipRequestMapper;
//
//    @Resource
//    private ImUserDataMapper imUserDataMapper;
//
//
//    @Override
//    public List<FriendVo> list(String userId, String sequence) {
//        // 查询用户的好友关系列表
//        List<ImFriendshipPo> imFriendshipPoList = imFriendshipMapper.selectList(new QueryWrapper<ImFriendshipPo>().eq("owner_id", userId).gt("sequence", sequence));
//
//        List<FriendVo> friendVoList = new ArrayList<>();
//
//        if (ObjectUtil.isNotEmpty(imFriendshipPoList) && !imFriendshipPoList.isEmpty()) {
//            // 查询好友的用户数据列表
//            List<ImUserDataPo> imUserDataPoList = imUserDataMapper.selectBatchIds(imFriendshipPoList.stream()
//                    .map(ImFriendshipPo::getToId)
//                    .collect(Collectors.toList()));
//
//            // 将ImUserData转换为FriendVo并设置好友关系的序列
//            friendVoList = imFriendshipPoList.stream()
//                    .flatMap(imFriendshipPo -> imUserDataPoList.stream()
//                            .filter(imUserData -> imUserData.getUserId().equals(imFriendshipPo.getToId()))
//                            .map(imUserData -> {
//                                FriendVo friendVo = new FriendVo();
//                                BeanUtils.copyProperties(imUserData, friendVo);
//                                friendVo.setUserId(userId);
//                                friendVo.setFriendId(imUserData.getUserId());
//                                friendVo.setBlack(imFriendshipPo.getBlack());
//                                friendVo.setAlias(imFriendshipPo.getRemark());
//                                friendVo.setSequence(imFriendshipPo.getSequence());
//                                return friendVo;
//                            }))
//                    .collect(Collectors.toList());
//        }
//
//        return friendVoList;
//    }
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





