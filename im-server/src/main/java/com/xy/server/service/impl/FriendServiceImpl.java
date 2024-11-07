package com.xy.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.imcore.enums.IMStatus;
import com.xy.server.domain.dto.FriendDto;
import com.xy.server.domain.dto.FriendRequestDto;
import com.xy.server.domain.dto.FriendshipRequestDto;
import com.xy.server.domain.po.ImFriendshipPo;
import com.xy.server.domain.po.ImFriendshipRequestPo;
import com.xy.server.domain.po.ImUserDataPo;
import com.xy.server.domain.vo.FriendVo;
import com.xy.server.domain.vo.FriendshipRequestVo;
import com.xy.server.mapper.ImFriendshipMapper;
import com.xy.server.mapper.ImFriendshipRequestMapper;
import com.xy.server.mapper.ImUserDataMapper;
import com.xy.server.service.FriendService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class FriendServiceImpl implements FriendService {

    @Resource
    private ImFriendshipMapper imFriendshipMapper;

    @Resource
    private ImFriendshipRequestMapper imFriendshipRequestMapper;

    @Resource
    private ImUserDataMapper imUserDataMapper;


    @Override
    public List<FriendVo> list(String user_id, String sequence) {
        // 查询用户的好友关系列表
        List<ImFriendshipPo> imFriendshipPoList = imFriendshipMapper.selectList(new QueryWrapper<ImFriendshipPo>().eq("owner_id", user_id).gt("sequence", sequence));

        List<FriendVo> friendVoList = new ArrayList<>();

        if (ObjectUtil.isNotEmpty(imFriendshipPoList) && !imFriendshipPoList.isEmpty()) {
            // 查询好友的用户数据列表
            List<ImUserDataPo> imUserDataPoList = imUserDataMapper.selectBatchIds(imFriendshipPoList.stream()
                    .map(ImFriendshipPo::getTo_id)
                    .collect(Collectors.toList()));

            // 将ImUserData转换为FriendVo并设置好友关系的序列
            friendVoList = imFriendshipPoList.stream()
                    .flatMap(imFriendshipPo -> imUserDataPoList.stream()
                            .filter(imUserData -> imUserData.getUser_id().equals(imFriendshipPo.getTo_id()))
                            .map(imUserData -> {
                                FriendVo friendVo = new FriendVo();
                                BeanUtils.copyProperties(imUserData, friendVo);
                                friendVo.setUser_id(user_id);
                                friendVo.setFriend_id(imUserData.getUser_id());
                                friendVo.setBlack(imFriendshipPo.getBlack());
                                friendVo.setAlias(imFriendshipPo.getRemark());
                                friendVo.setSequence(imFriendshipPo.getSequence());
                                return friendVo;
                            }))
                    .collect(Collectors.toList());
        }

        return friendVoList;
    }

    @Override
    public FriendVo findFriend(FriendDto friendDto) {

        QueryWrapper<ImFriendshipPo> imFriendshipQuery = new QueryWrapper<>();
        imFriendshipQuery.eq("owner_id", friendDto.getFrom_id());
        imFriendshipQuery.eq("to_id", friendDto.getTo_id());
        ImFriendshipPo imFriendshipPo = imFriendshipMapper.selectOne(imFriendshipQuery);

        FriendVo friendVo = new FriendVo();

        QueryWrapper<ImUserDataPo> imUserDataQuery = new QueryWrapper<>();
        imUserDataQuery.eq("user_id", friendDto.getTo_id());
        ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(imUserDataQuery);

        BeanUtils.copyProperties(imUserDataPo, friendVo);

        if (ObjectUtil.isNotEmpty(imFriendshipPo)) {
            friendVo.setFlag(IMStatus.YES.getCode());
        } else {
            friendVo.setFlag(IMStatus.NO.getCode());
        }

        return friendVo;
    }

    @Override
    @Transactional
    public void addFriend(FriendRequestDto friendRequestDto) {

        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();

        imFriendshipRequestQuery.eq("from_id", friendRequestDto.getFrom_id())
                .eq("to_id", friendRequestDto.getTo_id());

        if (ObjectUtil.isEmpty(imFriendshipRequestMapper.selectOne(imFriendshipRequestQuery))) {

            ImFriendshipRequestPo imFriendshipRequestPo = new ImFriendshipRequestPo();

            String id = UUID.randomUUID().toString();

            BeanUtil.copyProperties(friendRequestDto, imFriendshipRequestPo);

            imFriendshipRequestPo.setId(id)
                    .setRead_status(IMStatus.NO.getCode())
                    .setApprove_status(IMStatus.NO.getCode())
            ;

            imFriendshipRequestMapper.insert(imFriendshipRequestPo);
        }
    }

    @Override
    public List<FriendshipRequestVo> request(String user_id) {

        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();

        imFriendshipRequestQuery.eq("from_id", user_id);

        List<ImFriendshipRequestPo> imFriendshipRequestPoList = imFriendshipRequestMapper.selectList(imFriendshipRequestQuery);

        List<FriendshipRequestVo> friendshipRequestVoList = new ArrayList<>();

        imFriendshipRequestPoList.stream().forEach(imFriendshipRequestPo -> {

            FriendshipRequestVo FriendshipRequestVo = new FriendshipRequestVo();

            BeanUtils.copyProperties(imFriendshipRequestPo, FriendshipRequestVo);

            QueryWrapper<ImUserDataPo> imUserDataQuery = new QueryWrapper<>();
            imUserDataQuery.eq("user_id", imFriendshipRequestPo.getFrom_id());
            ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(imUserDataQuery);

            FriendshipRequestVo
                    .setName(imUserDataPo.getName())
                    .setAvatar(imUserDataPo.getAvatar());

            friendshipRequestVoList.add(FriendshipRequestVo);
        });

        return friendshipRequestVoList;
    }

    @Override
    @Transactional
    public void approveFriend(FriendshipRequestDto friendshipRequestDto) {

        String fromId = friendshipRequestDto.getFrom_id();

        String toId = friendshipRequestDto.getTo_id();

        // 被申请方审批通过构建好友关系
        if (friendshipRequestDto.getApprove_status().equals(IMStatus.YES.getCode())) {

            QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();

            imFriendshipRequestQuery.eq("id", friendshipRequestDto.getId());

            // 查询申请方备注
            ImFriendshipRequestPo imFriendshipRequestPo = imFriendshipRequestMapper.selectOne(imFriendshipRequestQuery);

            bindFriend(fromId, toId, imFriendshipRequestPo.getRemark());

            bindFriend(toId, fromId, friendshipRequestDto.getRemark());

            imFriendshipRequestPo
                    .setRead_status(IMStatus.YES.getCode())
                    .setApprove_status(IMStatus.YES.getCode())
            ;

            imFriendshipRequestMapper.updateById(imFriendshipRequestPo);
        }
    }


    /**
     * 建立好友关系
     *
     * @param fromId 申请方
     * @param toId   被申请方
     * @param remark 备注
     */
    public void bindFriend(String fromId, String toId, String remark) {

        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda().eq(ImFriendshipPo::getOwner_id, fromId).eq(ImFriendshipPo::getTo_id, toId);

        if (imFriendshipMapper.selectList(queryWrapper).size() == 0) {

            // 申请方对象
            ImFriendshipPo fromFriendship = new ImFriendshipPo()
                    .setOwner_id(fromId)
                    .setTo_id(toId)
                    .setRemark(remark)
                    .setStatus(IMStatus.YES.getCode())
                    .setBlack(IMStatus.YES.getCode())
                    .setSequence(new Date().getTime());

            imFriendshipMapper.insert(fromFriendship);
        }
    }


    /**
     * 删除好友，单向解除好友关系
     *
     * @param friendDto
     */
    public void delFriend(FriendDto friendDto) {

        String fromId = friendDto.getFrom_id();

        String toId = friendDto.getTo_id();

        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();

        queryWrapper.lambda().eq(ImFriendshipPo::getOwner_id, fromId).eq(ImFriendshipPo::getTo_id, toId);

        if (imFriendshipMapper.selectList(queryWrapper).size() > 0) {

            imFriendshipMapper.delete(queryWrapper);
        }
    }


}





