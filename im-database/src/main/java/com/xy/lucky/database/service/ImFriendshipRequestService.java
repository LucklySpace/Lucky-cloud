package com.xy.lucky.database.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.database.mapper.ImFriendshipRequestMapper;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.dubbo.api.database.friend.ImFriendshipRequestDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;


@DubboService
public class ImFriendshipRequestService extends ServiceImpl<ImFriendshipRequestMapper, ImFriendshipRequestPo>
        implements ImFriendshipRequestDubboService, IService<ImFriendshipRequestPo> {

    @Resource
    private ImFriendshipRequestMapper imFriendshipRequestMapper;

    public List<ImFriendshipRequestPo> selectList(String userId) {
        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();
        imFriendshipRequestQuery.eq("to_id", userId);
        return this.list(imFriendshipRequestQuery);
    }

    public ImFriendshipRequestPo selectOne(ImFriendshipRequestPo requestPo) {

        QueryWrapper<ImFriendshipRequestPo> imFriendshipRequestQuery = new QueryWrapper<>();

        if (StringUtils.hasText(requestPo.getId())) {
            imFriendshipRequestQuery.eq("id", requestPo.getId());
        }
        if (StringUtils.hasText(requestPo.getFromId()) && StringUtils.hasText(requestPo.getToId())) {
            imFriendshipRequestQuery.eq("from_id", requestPo.getFromId()).and(wrapper -> wrapper.eq("to_id", requestPo.getToId()));
        }

        return this.getOne(imFriendshipRequestQuery);
    }

    public Boolean insert(ImFriendshipRequestPo requestPo) {
        return save(requestPo);
    }

    public Boolean update(ImFriendshipRequestPo requestPo) {
        return this.updateById(requestPo);
    }

    public Boolean updateStatus(String requestId, Integer status) {
        UpdateWrapper<ImFriendshipRequestPo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", requestId).set("approve_status", status);
        return this.update(null, updateWrapper);
    }

    public Boolean deleteById(String requestId) {
        return this.removeById(requestId);
    }


}




