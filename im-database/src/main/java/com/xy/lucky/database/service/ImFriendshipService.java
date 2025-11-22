package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImFriendshipMapper;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.dubbo.api.database.friend.ImFriendshipDubboService;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class ImFriendshipService extends ServiceImpl<ImFriendshipMapper, ImFriendshipPo>
        implements ImFriendshipDubboService, IService<ImFriendshipPo> {

    @Resource
    private ImFriendshipMapper imFriendshipMapper;


    public List<ImFriendshipPo> selectList(String ownerId, Long sequence) {
        return imFriendshipMapper.selectFriendList(ownerId, sequence);
    }


    public List<ImFriendshipPo> selectByIds(String ownerId, List<String> ids) {
        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("owner_id", ownerId).in("to_id", ids);
        return this.list(queryWrapper);
    }


    public ImFriendshipPo selectOne(String ownerId, String toId) {
        QueryWrapper<ImFriendshipPo> query = new QueryWrapper<>();
        query.eq("owner_id", ownerId)
                .eq("to_id", toId);
        return this.getOne(query);
    }


    public Boolean insert(ImFriendshipPo friendship) {
        return this.save(friendship);
    }

    public Boolean update(ImFriendshipPo friendship) {
        return this.updateById(friendship);
    }


    public Boolean delete(String ownerId, String friendId) {
        UpdateWrapper<ImFriendshipPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("owner_id", ownerId).eq("to_id", friendId);
        updateWrapper.set("sequence", DateTimeUtils.getCurrentUTCTimestamp());
        updateWrapper.set("del_flag", 0);
        return this.update(updateWrapper);
    }
}