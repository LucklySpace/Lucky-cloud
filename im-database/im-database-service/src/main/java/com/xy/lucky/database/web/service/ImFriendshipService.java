package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImFriendshipMapper;
import com.xy.lucky.domain.BasePo;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.rpc.api.database.friend.ImFriendshipDubboService;
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
        Wrapper<ImFriendshipPo> queryWrapper = Wrappers.<ImFriendshipPo>lambdaQuery()
                .eq(ImFriendshipPo::getOwnerId, ownerId)
                .in(ImFriendshipPo::getToId, ids);
        return super.list(queryWrapper);
    }

    @Override
    public ImFriendshipPo queryOne(String ownerId, String toId) {
        Wrapper<ImFriendshipPo> queryWrapper = Wrappers.<ImFriendshipPo>lambdaQuery()
                .eq(ImFriendshipPo::getOwnerId, ownerId)
                .eq(ImFriendshipPo::getToId, toId);
        return super.getOne(queryWrapper);
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
        Wrapper<ImFriendshipPo> updateWrapper = Wrappers.<ImFriendshipPo>lambdaUpdate()
                .eq(ImFriendshipPo::getOwnerId, ownerId)
                .eq(ImFriendshipPo::getToId, friendId)
                .set(ImFriendshipPo::getSequence, DateTimeUtils.getCurrentUTCTimestamp())
                .set(BasePo::getDelFlag, 0);
        return super.update(updateWrapper);
    }
}
