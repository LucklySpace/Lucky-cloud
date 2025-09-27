package com.xy.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImFriendshipMapper;
import com.xy.database.mapper.ImFriendshipRequestMapper;
import com.xy.database.service.ImFriendshipService;
import com.xy.domain.po.ImFriendshipPo;
import com.xy.domain.po.ImFriendshipRequestPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_friendship】的数据库操作Service实现
 */
@Service
public class ImFriendshipServiceImpl extends ServiceImpl<ImFriendshipMapper, ImFriendshipPo>
        implements ImFriendshipService {


    @Resource
    private ImFriendshipMapper imFriendshipMapper;

    @Resource
    private ImFriendshipRequestMapper imFriendshipRequestMapper;

    @Override
    public List<ImFriendshipPo> list(String ownerId) {
        return imFriendshipMapper.selectFriendList(ownerId);
    }

    @Override
    public ImFriendshipPo getOne(String ownerId, String toId) {
        QueryWrapper<ImFriendshipPo> query = new QueryWrapper<>();
        query.eq("owner_id", ownerId)
                .eq("to_id", toId);
        return this.getOne(query);
    }

    @Override
    public void saveFriendRequest(ImFriendshipRequestPo request) {
        imFriendshipRequestMapper.insert(request);
    }

    @Override
    public void updateFriendRequestStatus(String requestId, Integer status) {
        UpdateWrapper<ImFriendshipRequestPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", requestId).set("approve_status", status);
        imFriendshipRequestMapper.update(null, updateWrapper);
    }

    @Override
    public void saveFriendship(ImFriendshipPo friendship) {
        this.save(friendship);
    }

    @Override
    public void deleteFriendship(String ownerId, String friendId) {
        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("owner_id", ownerId).eq("to_id", friendId);
        this.remove(queryWrapper);
    }

    @Override
    public List<ImFriendshipPo> getFriendshipList(String ownerId, List<String> ids) {
        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("owner_id", ownerId).in("to_id", ids);
        return this.list(queryWrapper);
    }

    @Override
    public void updateFriendRequest(ImFriendshipRequestPo request) {
        imFriendshipRequestMapper.updateById(request);
    }
}