package com.xy.lucky.database.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImFriendshipMapper;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.dubbo.web.api.database.friend.ImFriendshipDubboService;
import com.xy.lucky.utils.time.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImFriendshipService extends ServiceImpl<ImFriendshipMapper, ImFriendshipPo>
        implements ImFriendshipDubboService {

    private final ImFriendshipMapper imFriendshipMapper;

    @Override
    public List<ImFriendshipPo> queryList(String ownerId, Long sequence) {
        return imFriendshipMapper.selectFriendList(ownerId, sequence);
    }

    @Override
    public List<ImFriendshipPo> queryListByIds(String ownerId, List<String> ids) {
        QueryWrapper<ImFriendshipPo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("owner_id", ownerId).in("to_id", ids);
        return super.list(queryWrapper);
    }

    @Override
    public ImFriendshipPo queryOne(String ownerId, String toId) {
        QueryWrapper<ImFriendshipPo> query = new QueryWrapper<>();
        query.eq("owner_id", ownerId)
                .eq("to_id", toId);
        return super.getOne(query);
    }

    @Override
    public Boolean creat(ImFriendshipPo friendship) {
        return super.save(friendship);
    }

    @Override
    public Boolean modify(ImFriendshipPo friendship) {
        return super.updateById(friendship);
    }

    @Override
    public Boolean removeOne(String ownerId, String friendId) {
        UpdateWrapper<ImFriendshipPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("owner_id", ownerId).eq("to_id", friendId);
        updateWrapper.set("sequence", DateTimeUtils.getCurrentUTCTimestamp());
        updateWrapper.set("del_flag", 0);
        return super.update(updateWrapper);
    }
}
