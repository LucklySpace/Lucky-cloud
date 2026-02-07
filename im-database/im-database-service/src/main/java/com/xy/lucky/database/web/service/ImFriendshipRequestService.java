package com.xy.lucky.database.web.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.database.web.mapper.ImFriendshipRequestMapper;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.rpc.api.database.friend.ImFriendshipRequestDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;


@DubboService
@RequiredArgsConstructor
public class ImFriendshipRequestService extends ServiceImpl<ImFriendshipRequestMapper, ImFriendshipRequestPo>
        implements ImFriendshipRequestDubboService {

    private final ImFriendshipRequestMapper imFriendshipRequestMapper;

    @Override
    public List<ImFriendshipRequestPo> queryList(String userId) {
        Wrapper<ImFriendshipRequestPo> queryWrapper = Wrappers.<ImFriendshipRequestPo>lambdaQuery()
                .eq(ImFriendshipRequestPo::getToId, userId);
        return super.list(queryWrapper);
    }

    @Override
    public ImFriendshipRequestPo queryOne(ImFriendshipRequestPo requestPo) {

        LambdaQueryWrapper<ImFriendshipRequestPo> queryWrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(requestPo.getId())) {
            queryWrapper = queryWrapper.eq(ImFriendshipRequestPo::getId, requestPo.getId());
        }
        if (StringUtils.hasText(requestPo.getFromId()) && StringUtils.hasText(requestPo.getToId())) {
            queryWrapper = queryWrapper.eq(ImFriendshipRequestPo::getFromId, requestPo.getFromId())
                    .and(wrapper -> wrapper.eq(ImFriendshipRequestPo::getToId, requestPo.getToId()));
        }
        return super.getOne(queryWrapper);
    }

    @Override
    public Boolean creat(ImFriendshipRequestPo requestPo) {
        return super.save(requestPo);
    }

    @Override
    public Boolean modify(ImFriendshipRequestPo requestPo) {
        return super.updateById(requestPo);
    }

    @Override
    public Boolean modifyStatus(String requestId, Integer status) {
        Wrapper<ImFriendshipRequestPo> updateWrapper = Wrappers.<ImFriendshipRequestPo>lambdaUpdate()
                .eq(ImFriendshipRequestPo::getId, requestId)
                .set(ImFriendshipRequestPo::getApproveStatus, status);
        return super.update(null, updateWrapper);
    }

    @Override
    public Boolean removeOne(String requestId) {
        return super.removeById(requestId);
    }


}




