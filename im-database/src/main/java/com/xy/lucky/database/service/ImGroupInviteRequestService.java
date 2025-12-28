package com.xy.lucky.database.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImGroupInviteRequestMapper;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import com.xy.lucky.dubbo.api.database.group.ImGroupInviteRequestDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImGroupInviteRequestService extends ServiceImpl<ImGroupInviteRequestMapper, ImGroupInviteRequestPo> implements ImGroupInviteRequestDubboService {

    private final ImGroupInviteRequestMapper imGroupInviteRequestMapper;

    @Override
    public List<ImGroupInviteRequestPo> queryList(String userId) {
        QueryWrapper<ImGroupInviteRequestPo> groupInviteRequestPoQueryWrapper = new QueryWrapper<>();
        groupInviteRequestPoQueryWrapper.eq("to_id", userId);
        return super.list(groupInviteRequestPoQueryWrapper);
    }

    @Override
    public ImGroupInviteRequestPo queryOne(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return super.getById(imGroupInviteRequestPo);
    }

    @Override
    public Boolean creat(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return super.save(imGroupInviteRequestPo);
    }

    @Override
    public Boolean modify(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return super.updateById(imGroupInviteRequestPo);
    }

    @Override
    public Boolean removeOne(String requestId) {
        return super.removeById(requestId);
    }

    @Override
    public Boolean creatBatch(List<ImGroupInviteRequestPo> list) {
        return !imGroupInviteRequestMapper.insert(list).isEmpty();
    }
}
