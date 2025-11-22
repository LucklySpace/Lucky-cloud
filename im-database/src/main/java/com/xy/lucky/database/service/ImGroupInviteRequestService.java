package com.xy.lucky.database.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.mapper.ImGroupInviteRequestMapper;
import com.xy.lucky.domain.po.ImGroupInviteRequestPo;
import com.xy.lucky.dubbo.api.database.group.ImGroupInviteRequestDubboService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImGroupInviteRequestService extends ServiceImpl<ImGroupInviteRequestMapper, ImGroupInviteRequestPo> implements ImGroupInviteRequestDubboService, IService<ImGroupInviteRequestPo> {

    @Resource
    private ImGroupInviteRequestMapper imGroupInviteRequestMapper;

    public List<ImGroupInviteRequestPo> selectList(String userId) {
        QueryWrapper<ImGroupInviteRequestPo> groupInviteRequestPoQueryWrapper = new QueryWrapper<>();
        groupInviteRequestPoQueryWrapper.eq("to_id", userId);
        return this.list(groupInviteRequestPoQueryWrapper);
    }

    public ImGroupInviteRequestPo selectOne(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return this.getById(imGroupInviteRequestPo);
    }

    public Boolean insert(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return this.save(imGroupInviteRequestPo);
    }

    public Boolean update(ImGroupInviteRequestPo imGroupInviteRequestPo) {
        return this.updateById(imGroupInviteRequestPo);
    }

    public Boolean deleteById(String requestId) {
        return this.removeById(requestId);
    }

    @Override
    public Boolean batchInsert(List<ImGroupInviteRequestPo> list) {
        return !imGroupInviteRequestMapper.insert(list).isEmpty();
    }
}
