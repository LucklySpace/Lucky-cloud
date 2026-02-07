package com.xy.lucky.database.web.service;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.ImGroupInviteRequestMapper;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import com.xy.lucky.rpc.api.database.group.ImGroupInviteRequestDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class ImGroupInviteRequestService extends ServiceImpl<ImGroupInviteRequestMapper, ImGroupInviteRequestPo> implements ImGroupInviteRequestDubboService {

    private final ImGroupInviteRequestMapper imGroupInviteRequestMapper;

    @Override
    public List<ImGroupInviteRequestPo> queryList(String userId) {
        Wrapper<ImGroupInviteRequestPo> queryWrapper = Wrappers.<ImGroupInviteRequestPo>lambdaQuery()
                .eq(ImGroupInviteRequestPo::getToId, userId);
        return super.list(queryWrapper);
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
