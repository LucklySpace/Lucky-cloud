package com.xy.lucky.database.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.database.mapper.ImFriendshipRequestMapper;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.dubbo.web.api.database.friend.ImFriendshipRequestDubboService;
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
        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
        imFriendshipRequestQuery.eq("to_id", userId);
        return super.list(imFriendshipRequestQuery);
    }

    @Override
    public ImFriendshipRequestPo queryOne(ImFriendshipRequestPo requestPo) {

        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();

        if (StringUtils.hasText(requestPo.getId())) {
            imFriendshipRequestQuery.eq("id", requestPo.getId());
        }
        if (StringUtils.hasText(requestPo.getFromId()) && StringUtils.hasText(requestPo.getToId())) {
            imFriendshipRequestQuery.eq("from_id", requestPo.getFromId()).and(wrapper -> wrapper.eq("to_id", requestPo.getToId()));
        }

        return super.getOne(imFriendshipRequestQuery);
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
        UpdateWrapper<ImFriendshipRequestPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", requestId).set("approve_status", status);
        return super.update(null, updateWrapper);
    }

    @Override
    public Boolean removeOne(String requestId) {
        return super.removeById(requestId);
    }


}




